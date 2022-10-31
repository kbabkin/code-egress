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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bt.code.egress.Config;
import com.google.common.collect.Sets;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
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
public class XgressSourceService {

    //to match with egress-$(date +"%Y-%m-%d_%H-%M-%S_%3N"):
    private static final String TIMESTAMPED_TAG_DATETIME_FORMAT = "yyyy-MM-dd_HH-mm-ss_SSS";
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(TIMESTAMPED_TAG_DATETIME_FORMAT);

    @SneakyThrows
    public void checkBranchCheckout(String dir, String branch) {
        try (Repository sourceRepo = getRepo(dir)) {
            String fullBranch = sourceRepo.getFullBranch();
            checkArgument(String.format("refs/heads/%s", branch).equals(fullBranch),
                    "Branch 'refs/heads/%s' is expected to be checked out to %s, but '%s' is checked out",
                    branch, dir, fullBranch);
            try (
                    Git git = new Git(sourceRepo);
            ) {
                Status status = git.status().call();
                if (!status.isClean() || status.hasUncommittedChanges()) {
                    log.warn("{} work tree status: isClean={}, hasUncommittedChanges={}");
                    log.warn("Status: {}", status);
                }
            }
        }
    }

    @SneakyThrows
    public void checkTagCheckout(String dir, String tag, String branch) {
        try (Repository sourceRepo = getRepo(dir)) {
            Ref tagRef = sourceRepo.exactRef(R_TAGS + tag);
            checkArgument(tagRef != null,
                    "Tag %s not found, available tags: %s",
                    tag, Sets.newTreeSet(sourceRepo.getTags().keySet()));
            Ref headRef = getHeadRef(sourceRepo, branch);

            checkArgument(tagRef.getObjectId().equals(headRef.getObjectId()),
                    "Tag '%s' with object id %s is expected to be checked out to %s, but object id  %s is checked out",
                    tag, tagRef.getObjectId(), dir, headRef.getObjectId());
        }
    }

    public void checkDirectory(String dir, String comment) {
        Path path = Paths.get(dir);
        checkArgument(Files.isDirectory(path), "specified %s (%s) is not an existing directory",
                comment, dir);
    }


    @SneakyThrows
    public List<ChangeAction> getXgressChanges(String source, String branch, String latestTag) {
        //TODO how to handle TAG case?
        //TODO how to support ranges for github tags?
        //temporary using dummy!
        String sourceBranchName = "dummy";
        Path sourcePath = Paths.get(source);
        checkArgument(Files.isDirectory(sourcePath), "specified source repo (%s) is not directory", source);

        try (Repository sourceRepo = getRepo(source)) {
            Ref headRef = getHeadRef(sourceRepo, branch);

            Ref latestRef = sourceRepo.exactRef(R_TAGS + latestTag);
            checkState(latestRef != null, "tag %s not found in repository %s",
                    latestTag, sourceRepo.getDirectory());
            log.info("found tag {} in source repository, object-id={}", latestTag, latestRef.getObjectId().name());

            try (
                    Git git = new Git(sourceRepo);
                    ObjectReader reader = sourceRepo.newObjectReader()
            ) {
                log.info("getting changes between {} and {} in branch {}", latestTag, HEAD, sourceBranchName);
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
    public void completeEgress(String source, String branch, String latestTag, String timestampedTagPrefix) {
        checkSource(source);

        try (Repository sourceRepo = getRepo(source)) {
            try (Git git = new Git(sourceRepo)) {
                addTag(git, latestTag, branch);
                addTag(git, generateTimestampedTagName(latestTag, timestampedTagPrefix), branch);
            }
        }
    }

    @SneakyThrows
    private void addTag(Git git, String tag, String branch) {
        log.info("setting tag {} to {} for {} branch", tag, HEAD, branch);
        Ref tagRef = git.tag()
                .setName(tag)
                .setForceUpdate(true)
                .call();
        log.info("tag {} set to {} of {} branch, commit-id={}",
                tag, HEAD, branch, tagRef.getObjectId().name());
    }

    private String generateTimestampedTagName(String tag, String prefix) {
        return prefix + dateTimeFormatter.format(LocalDateTime.now());
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
    private Ref getHeadRef(Repository repository, String branch) {
        log.info("Getting {} for {} ...", HEAD, repository.getDirectory());
        Ref headRef = repository.exactRef(HEAD);
        checkState(headRef != null, "could not not read repository HEAD");

        String branchRefString = R_HEADS + branch;
        log.info("Getting {} for {} ...", branchRefString, repository.getDirectory());
        Ref branchRef = repository.exactRef(R_HEADS + branch);
        checkState(branchRef != null, "could not not find branch %s", branch);
        checkState(Objects.equals(headRef.getObjectId(), branchRef.getObjectId()),
                "current branch is not %s", branch);

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