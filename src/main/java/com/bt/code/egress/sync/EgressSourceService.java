package com.bt.code.egress.sync;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_TAGS;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.stereotype.Service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;;

@Service
@Slf4j
public class EgressSourceService {
    private static final String BRANCH_NAME = "main"; 
    private static final String LATEST_TAG = "egress-latest"; 

    @SneakyThrows
    public List<ChangeAction> getEgressChanges(String source) {
        Path sourcePath = Paths.get(source);
        checkArgument(Files.isDirectory(sourcePath), "specified source repo (%s) is not directory", source);

        try (Repository sourceRepo = getRepo(source)) {
            Ref headRef = getHeadRef(sourceRepo);

            Ref latestRef = sourceRepo.exactRef(R_TAGS + LATEST_TAG);
            checkState(latestRef != null, "tag {} not found in repository", LATEST_TAG);
            log.info("found tag {} in source repository, object-id={}", LATEST_TAG, latestRef.getObjectId().name());

            try (
                Git git = new Git(sourceRepo);
                ObjectReader reader = sourceRepo.newObjectReader()
            ) {
                log.info("getting changes between {} and {} in branch {}", LATEST_TAG, HEAD, BRANCH_NAME);
                List<DiffEntry> diffs = git.diff()
                    .setOldTree(getTreeParser(sourceRepo, reader, latestRef))
                    .setNewTree(getTreeParser(sourceRepo, reader, headRef))
                    .call();
                log.info("found {} diff(s)", diffs.size());
                
                List<ChangeAction> changeActions = diffs.stream()
                    .flatMap(this::toChanges)
                    .collect(Collectors.toList());

                return changeActions;
            }
        }
    }

    @SneakyThrows
    public void completeEgress(String source) {
        checkSource(source);

        try (Repository sourceRepo = getRepo(source)) {
            try (Git git = new Git(sourceRepo)) {
                log.info("setting tag {} to {} for {} branch", LATEST_TAG, HEAD, BRANCH_NAME);
                Ref tagRef = git.tag()
                    .setName(LATEST_TAG)
                    .setForceUpdate(true)
                    .call();
                log.info("tag {} set to {} of {} branch, commit-id={}", LATEST_TAG, HEAD, BRANCH_NAME, tagRef.getObjectId().name());
            }
        }
    }

    private void checkSource(String source) {
        Path repoPath = Paths.get(source);
        checkArgument(Files.isDirectory(repoPath), "specified source repo (%s) is not directory", source);
    }

    @SneakyThrows
    private static Repository getRepo(String path) {
        return new FileRepositoryBuilder()
            .setGitDir(new File(path, ".git"))
            .readEnvironment()
            .build();
    }

    @SneakyThrows
    private static Ref getHeadRef(Repository repository) {
        Ref headRef = repository.exactRef(HEAD);
        checkState(headRef != null, "could not not read repository HEAD");

        Ref branchRef = repository.exactRef(R_HEADS + BRANCH_NAME);
        checkState(branchRef != null, "could not not find branch %s", BRANCH_NAME);
        checkState(Objects.equals(headRef.getObjectId(), branchRef.getObjectId()), "current branch is not %s", BRANCH_NAME);

        return headRef;
    }
    
    @SneakyThrows
    private static AbstractTreeIterator getTreeParser(Repository repository, ObjectReader reader, Ref ref) {
        if (ref == null) {
            return null;
        }

        ObjectId treeId = getTreeId(repository, ref);
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        treeParser.reset(reader, treeId);
        return treeParser;
    }

    @SneakyThrows
    private static ObjectId getTreeId(Repository repository, Ref ref) {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(ref.getObjectId());
            return commit.getTree().getId();
        }
    }

    private Stream<ChangeAction> toChanges(DiffEntry diff) {
        switch (diff.getChangeType()) {
            case ADD:
            case MODIFY:
            case COPY:
                return Stream.of(
                    ChangeAction.copy(diff.getNewPath())
                );
            case DELETE:
                return Stream.of(
                    ChangeAction.delete(diff.getOldPath())
                );
            case RENAME:
                return Stream.of(
                    ChangeAction.delete(diff.getOldPath()),
                    ChangeAction.copy(diff.getNewPath())
                );
            default:
                checkArgument(false, "unknown changeType %s", diff.getChangeType());
                return Stream.empty(); // not reachable
        }
    }
}