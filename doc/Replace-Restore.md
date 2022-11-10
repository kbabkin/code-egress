# Replace/Restore

Replace (Mask) confidential details in code
in internal repository before publishing it to external repository.
Restore (Unmask) changes done in external repository for use in internal repository.
Sequence of multiple Replace (Mask) and/or Restore (Unmask) operations produces
reasonably consistent results.

Replace/Restore input configuration and output reports heavily use CSV format.

# Table of Contents

- [Features](#features)
- [High Level Process](#high-level-process)
- [Report / Instruction File](#report--instruction-file)
- [Configuration](#configuration)
- [Run](#run)
- [File Processing](#file-processing)
- [Text Processing](#text-processing)
- [CSV Column Template Processing](#csv-column-template-processing)
- [ZIP Processing](#zip-processing)
- [Tips and Tricks](#tips-and-tricks)

# Features

- Replace (Mask)
    - Repeated Replace (Mask) produces same result
    - Match file path with wildcards
    - Exact word match, Java regexp match
    - Fill CSV file columns by template
    - Process ZIP archives
- Restore (Unmask) relies on report of Replace (Mask)
- Identify false-positives in Instruction file
- Report matches and changes

# High Level Process

Replace (Mask) operation can be optionally followed by Restore (Unmask) operation
to integrate changes done over masked code.

## Replace (Mask)

- Collect dictionary of guarded words to ``word-guard-value.csv``.
- Run scan (direction - replace, mode - preview).
  Review produced report files and replaced files content in ``preview`` folder.
  Repeat scan and review steps as needed.
    - Review report file ``replace-report.csv`` to fix configurations and mark false-positives.
      Copy report file lines with filled **Allow** column to ``replace-instruction-lines.csv`` for particular
      false-positives,
      or to ``replace-instruction.csv`` for common false-positives with wider context and file path matching.
      All report lines should be marked as true-positive or false-positive to complete review.
    - Check ``generated-replacement.csv``. When exact replacement value is required,
      for example to fit to fixed length column in database,
      add word with replacement to ``word-guard-value.csv``.
    - In case there are CSV Column Templates configured, check ``csv-dictionary-candidate.csv``
      and copy required confidential words to ``word-guard-value.csv``.
    - Check ``file-error.csv``, for example some files can be added to ``file-ignore.csv``
- Run replace (direction - replace, mode - inplace) according to collected configuration.
  Add JVM parameter ``-Dwrite.inplace=true``
- If Restore (Unmask) is planned later, copy ``replace-instruction.csv``
  from ``restore-instruction-last.csv`` or ``restore-instruction-cumulative.csv`` as appropriate.

## Restore (Unmask)

- Take ``replace-instruction.csv`` from previous Replace (Mask) operation.
  If it is not available, Replace (Mask) can be re-done to produce that report.
- Run Restore (Unmask) (direction - restore, mode - inplace)
  Add JVM parameters ``-Dscan.direction=restore -Dwrite.inplace=true``
- Check results and restart restore if required.
    - Check ``restore-generated-replacement.csv`` for missing or ambiguous replacements.
      Provide correct value in ``replace-instruction.csv``
    - If some replacement needs to be skipped, it can be marked as **Allow=true**
      in ``replace-instruction.csv``

# Report / Instruction File

Report file is a crucial part of the process.
Report contains occurrences of guarded words, one per line, in CSV format.
File can be viewed and edited by IDE plugin, Excel, plain text editor.
Replace uses same format as report file with filled column **Allow** as input configuration.

## Report File Format

Report file is stored as ``replace-report.csv``. Example report row:

| Allow | Text          | Context                | File                      | Line | Replacement              | Comment                                    |
|-------|---------------|------------------------|---------------------------|------|--------------------------|--------------------------------------------|
|       | 192.168.10.11 | check ip 192.168.10.11 | resources/sample-file.txt | 9    | h1163712847.domain.local | Pattern \d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3} |

## Instruction Matching

Replace instruction file uses same format as report file.
Use ``replace-instruction-lines.csv`` for particular false-positives with exact context and file match
or ``replace-instruction.csv`` for common false-positives with wider context and file path matching.
These files can be prepared manually or by copying lines from report file ``replace-report.csv`` with **Allow** column
filling.

How columns are used during Replace:

- **"Allow"** mark false-positive after manual review:
    - **"true"** - marks guarded word occurrence as false-positive, to be excluded from replacement. Values "yes", "y"
      , "1" are also accepted.
    - **"false"** - reviewed, true-positive, not allowed, will be replaced. Value is kept in next scan, used to
      differentiate reviewed and not reviewed lines.
    - empty value - not allowed, not reviewed, will be replaced.
- **Text** - found guarded word.
- **Context** - few characters before and after Text.
  Can be exact value from report or more narrow string.
  If multiple rows matched, wider context is preferred.
  If it is empty, row matches any context.
- **File** - file path matching, wildcards allowed.
  If it is empty, row matches any file.
- **Line**, **Replacement**, **Comment** - ignored.

Example matching rows:

| Allow | Text          | Context                | File                      | Line | Replacement       | Comment                                     |
|-------|---------------|------------------------|---------------------------|------|-------------------|---------------------------------------------|
| true  | 192.168.10.11 | check ip 192.168.10.11 | resources/sample-file.txt | 9    | h116.domain.local | Line copied from report                     |
| true  | 192.168.10.11 | check ip 192.168.10.11 | resources/sample-file.txt |      |                   | Removed unused fields                       |
| true  | 192.168.10.11 | ip 192.168.10.11       | resources/sample-file.txt |      |                   | Partial context                             |
| true  | 192.168.10.11 |                        | resources/sample-file.txt |      |                   | In file with any context                    |
| true  | 192.168.10.11 | ip 192.168.10.11       | **/sample-*.txt           |      |                   | File path matching                          |
| true  |               |                        | resources/sample-file.txt |      |                   | Ignore whole file due to previous exception |

# Configuration

Scan project here means set of configuration and output files per project being scanned.
Opposite to project code, scan project should not be published.

Check comments in [src/main/resources/application.yml](../src/main/resources/application.yml)
and [scan-project/sample/config/scan-application.yml](../scan-project/sample/config/scan-application.yml)

## Sample project

Sample project can be used as base for your custom scan project. It also can se used directly at first steps.

- Edit [scan-project/sample/config/scan-application.yml](../scan-project/sample/config/scan-application.yml)

```yaml
read:
  # !!! Project folder to be scanned - provide actual value !!!
  folder: "/work/project/scanned-sample"
```

- Start ``com.bt.code.egress.App``

Folder structure

```
scan-project                             # folder to collect scan configuration for mutliple repos
  sample                                 # one repo/project configuration
    config                               # configuration to be persisted in Git among scans and releases
      scan-application.yml               # main configuration file
      file-ignore.csv                    # file folder (ending with '/') and file name patterns to exclude from scan
      word-guard-value.csv               # dictionary to find, optionally with replacement
      word-guard-pattern.csv             # patterns to find, optionally with replacement template
      replace-instruction.csv            # common false-positives, utilizing context matching, file path patterns
      replace-instruction-lines.csv      # particular false-positives, copy-pasted from report
      restore-instruction.csv            # instructions to restore (replace back) from masked to unmasked values
    target                               # scan output files, ignored by Git and scan                        
      replace-report.csv                 # replace report file to manually identify false-positives
      csv-dictionary-candidate.csv       # non-replaced values from CSV "dictionary" column
      file-error.csv                     # file level messages and errors
      generated-replacement.csv          # can be adjusted and copied to dictionary
      restore-instruction-cumulative.csv # report from replace for future restore combined with config/restore-instruction.csv
      restore-instruction-last.csv       # report from replace for future restore, last run only 
      restore-report.csv                 # restore report file to identify restore issues 
      restore-generated-replacement.csv  # missing and ambigious restore values
      preview                            # folder for replaced files to check during review phase
      logs                               # scan logs
      
```

## Custom project

Create scan project per project being scanned. Its configuration can be stored in separate source control.

- Copy [scan-project/sample](../scan-project/sample), e.g. to ``scan-project/myproject``
- Edit ``read.folder`` in ``scan-project/myproject/config/scan-application.yml``
- Start ``-Dscan.project=scan-project/myproject com.bt.code.egress.App``

## Test configuration

Can be used to check how it works, verify configuration parts, reproduce bugs, etc.

- Start ``com.bt.code.egress.TestApp``

Test folders:

- src/test/resources/config - configuration.
- src/test/resources/testdata - test files - TXT, CSV, ZIP, etc.
- target/scan-project/target - output files.

# Run

Run with passing scan project location as JVM parameter

- Application in IDE
    - Class: ``com.bt.code.egress.App``
    - VM Parameters:``-Dscan.project=scan-project/myproject``
    - Create run configurations per scan project, per processing mode.
- Spring Boot Jar
    - ``java -jar code-egress.jar -Dscan.project=scan-project/myproject``

### Processing direction

- Replace. Mask confidential data. This is default mode, or add JVM
  option ``-Dscan.direction=replace``
- Restore. Unmask confidential data. Add JVM option ``-Dscan.direction=restore``

### Processing Mode

- Review mode. Code is not changed, changed files are placed to ``preview`` folder. This is default mode, or add JVM
  option ``-Dwrite.inplace=false``
- Inplace mode. Code is replaced inplace. Add JVM option ``-Dwrite.inplace=true``

Report files are generated in both modes.

# File Processing

- File path is matched
  by [Ant-like wildcards](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/util/AntPathMatcher.html)
    - Folder match ends with '/', e.g. ``**/target/``
    - File match does not end with '/', e.g. ``**/*.class``
- Add ignored files to ``file-ignore.csv``
- Check ``file-error.csv`` for file level messages and errors,
  like guarded words in file name or encoding issues.

# Text Processing

- Word Replacement Generation
    - Replacement value is generated by template for match by regexp
      or by default template when replacement/template is not provided.
      Template string can contain variable {hash}.
      For example, for configuration ``"[\\w[\\w.-]+@\\w+(\\.\\w+)+]": "u{hash}@mail.local"``
      in ``scan-application.yml``
      generated value can be ``u815489072@mail.local``.
    - Scan saves generated replacements to ``generated-replacement.csv``.
      Part of it contents can be copied to ``word-guard-value.csv`` to
        - Provide custom replacements, for example to store in short 2-3 characters database field.
        - Keep same replacements even if generating function is changed later.
- When there are conflicting matches, wider one is used.
  For example ``match@acme.com`` is wider than ``acme``, so its configuration will be applied.
- Only changed files are saved, both in Review and Cleanup processing modes.

# CSV Column Template Processing

CSV files can be configured to fill columns by template, even if no guarded words are found.

## CSV Column Template configuration per file type

```yaml
csv:
  enabled: true
  files:
    # CSV configuration per file type. File path is matched with wildcards
    - filename: "*accounts.csv"
      columns: # Columns to fill by template
        "name": "n{id}"       # fill by pattern
        "fullName": "fn{id}"  # {id} meas value from "id" column in same row 
        "email": ""           # clean
        "phone": "NA"         # fill with static value
      dictionary: # Columns to replace as plain text or to add to csv-dictionary-candidate.csv otherwise 
        "mnemonic": "mn{id}"
```

Column types:

- **columns:** are filled by template.
  Content of column filled by template is not restorable.
  Report contains only one line per CSV file, as described below.
  For example, for configuration **"name": "n{id}"** column **"name"** will contain
  character **"n"** concatenated with content of column **"id"** in same row.
- **dictionary:** columns are processed and reported according to usual Text matching per cell.
  Additionally, not replaced values are added to ``csv-dictionary-candidate.csv``.
- Values in not mentioned as **columns:** or **dictionary:**
  are processed and reported according to usual Text matching.
  Context in report contains column name as prefix.

## CSV Column Template in Report File

Report File combines results from CSV and Text processing

- Report File contains single row for file matched to CSV configuration.
    - **Context** column shows few examples of guarded words.
    - **Allow** column can be set, for example, you may set **Allow=true** for **Context=No guarded words found**.
- Columns not mentioned in CSV configuration are scanned as usual Text,
  and report contains a line for each matched cell.

### Example scenario

**Given** config

```yaml
word:
  guard:
    values:
      "acme": ""
      "acm": ""
csv:
  enabled: true
  files:
    - filename: "*LegalEntity*.csv"
      columns:
        "name": "n{leId}"
        "fullName": "fn{leId}"
        "mnemonic": ""
```

**When** scanned file *resources/LegalEntity1.csv* with content

| leId  | name   | fullName  | comment      | mnemonic |
|-------|--------|-----------|--------------|----------|
| 12345 | ACME   | Acme, Inc | Code ACM.E   | XCV123   |
| 11111 | n11111 | fn11111   | Code w654378 |          |

**Then** report is as following:

| Allow | Text                       | Context                    | File                        | Line | Replacement       | Comment              |
|-------|----------------------------|----------------------------|-----------------------------|------|-------------------|----------------------|
|       | csv:name,fullName,mnemonic | Acme, Inc and 1 more match | resources/LegalEntity1.csv  | 1    | n{leId},fn{leId}, | CSV Column Template  |
|       | acm                        | Code ACM.E                 | resources/LegalEntity1.csv  | 1    | w241563812        | Value acm            |

# ZIP Processing

Files inside ZIP archives are scanned and replaced in same manner as plain files.

In Intellij IDEA use **Ctrl+D** to compare ZIP files, 
or plugin to view ZIP content: https://plugins.jetbrains.com/plugin/9491-archive-browser

# Tips and Tricks

- If you want to explore implementation, start with class [LineReplacer](../src/main/java/com/bt/code/egress/process/LineReplacer.java)
- Use cases - see integration tests in **com.bt.code.egress.process** package.
- You can split review task in smaller parts by adding filters by module,
  e.g. ``**/mymodule/`` or by file type, e.g. ``**/*.csv``.
  Report format and file names in it will be the same, so later those additional filters can be removed.
