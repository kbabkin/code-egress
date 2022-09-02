# Overview

Project **code-egress** scans and cleans up confidential details from code before publishing it.
It maintains consistent results among multiple sequential scans and pre-release scans.

# Table of Contents

- [Features](#features)
- [High Level Process](#high-level-process)
- [Report File](#report-file)
- [Configuration](#configuration)
- [Run](#run)
- [File Processing](#file-processing)
- [Text Processing](#text-processing)
- [CSV Processing](#csv-processing)
- [ZIP Processing](#zip-processing)
- [Tips and Tricks](#tips-and-tricks)

# Features

- Matching file path with wildcards
- Word exact match, Java regexp match
- CSV files can be configured to fill columns by template
- ZIP archives processing

# High Level Process

- Collect dictionary of guarded words to ``word-guard-value.csv``.
- Scan code to produce report file ``code-report.csv`` and replaced files content in ``preview`` folder.
- Review report file to fix configurations and mark false-positives.
  Copy report file lines with filled **Allow** column to ``allow-report-lines.csv`` for particular false-positives,
  or to ``allow-report.csv`` for common false-positives with wider context and file path matching.
- Repeat scan and review steps as needed.
- Cleanup Code according to collected configuration. Add JVM parameter ``-Dwrite.inplace=true``

# Report File

Report file is a crucial part of the process.
Report contains occurrences of guarded words, one per line, in CSV format.
File can be viewed and edited by IDE plugin, Excel, plain text editor.
Scan uses same format as report file with filled column **Allow** as input configuration.

## Report File Format

Report file is stored as ``code-report.csv``. Example report row:

| Allow | Text          | Context                | File                      | Line | Replacement              | Comment                                    |
|-------|---------------|------------------------|---------------------------|------|--------------------------|--------------------------------------------|
|       | 192.168.10.11 | check ip 192.168.10.11 | resources/sample-file.txt | 9    | h1163712847.domain.local | Pattern \d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3} |

## Report File Matching

Scan uses same format as report file.
Use ``allow-report-lines.csv`` for particular false-positives with exact context and file match
or ``allow-report.csv`` for common false-positives with wider context and file path matching.
These files can be prepared manually or by copying lines from report file ``code-report.csv`` with **Allow** column
filling.

How columns are used during scans and cleanup:

- **"Allow"** mark false-positive after manual review:
    - **"true"** - marks guarded word occurrence as false-positive, to be excluded from replacement. Values "yes", "y"
      , "1" are also accepted.
    - **"false"** - reviewed, true-positive, not allowed, will be replaced. Value is kept in next scan, used to
      differentiate reviewed and not reviewed lines.
    - empty value - not allowed, not reviewed, will be replaced.
- **Text** - found guarded word.
- **Context** - few characters before and after Text.
  Can be exact value from report or more narrow string.
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
Check comments in [src/main/resources/application.yml](src/main/resources/application.yml)
and [scan-project/sample/config/scan-application.yml](scan-project/sample/config/scan-application.yml)

## Sample project

Sample project can be used as base for your custom scan project. It also can se used directly at first steps.

- Edit [scan-project/sample/config/scan-application.yml](scan-project/sample/config/scan-application.yml)

```
read:
  # !!! Project folder to be scanned - provide actual value !!!
  folder: "/work/project/scanned-sample"
```

- Start ``com.bt.code.egress.App``

Folder structure

```
scan-project                    # folder to collect scan configuration for mutliple repos
  sample                        # one repo/project configuration
    config                      # configuration to be persisted in Git among scans and releases
      scan-application.yml      # main configuration file
      file-ignore.csv           # file folder (ending with '/') and file name patterns to exclude from scan
      word-guard-value.csv      # dictionary to find, optionally with replacement
      word-guard-pattern.csv    # patterns to find, optionally with replacement template
      allow-report.csv          # common false-positives, utilizing context matching, file path patterns
      allow-report-lines.csv    # particular false-positives, copy-pasted from report
    target                      # scan output files, ignored by Git and scan                        
      code-report.csv           # report file to manually identify false-positives
      generated-replacement.csv # can be adjusted and copied to dictionary
      preview                   # folder for replaced files to check during review phase
      logs                      # scan logs
```

## Custom project

Create scan project per project being scanned. Its configuration can be stored in separate source control.

- Copy [scan-project/sample](scan-project/sample), e.g. to ``scan-project/myproject``
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

Processing Modes

- Review mode. Code is not changed, changed files are placed to ``preview`` folder. This is default mode, or add JVM
  option ``-Dwrite.inplace=false``
- Cleanup mode. Code is replaced inplace. Add JVM option ``-Dwrite.inplace=true``

Report file is generated in both modes.

# File Processing

- File path is matched
  by [Ant-like wildcards](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/util/AntPathMatcher.html)
    - Folder match ends with '/', e.g. ``**/target/``
    - File match does not end with '/', e.g. ``**/*.class``
- File level messages and errors are dumped to console and log when scan is completed
- Add ignored files to ``file-ignore.csv``

# Text Processing

- When there are conflicting matches, wider one is used.
  For example ``match@acme.com`` is wider than ``acme``, so its configuration will be applied.
- Word Replacement Generation
    - Replacement value is generated by template for match by regexp
      or by default template when replacement/template is not provided.
      Template string can contain variable {hash}.
      For example, for configuration ``"[\\w[\\w.-]+@\\w+(\\.\\w+)+]": "u{hash}@mail.local"``
      in ``scan-application.yml``
      generated value can be ``u815489072@mail.local``.
    - Scan saves generated replacements tp ``generated-replacement.csv``.
      Part of it contents can be copied to ``word-guard-value.csv`` to
        - Provide custom replacements, for example to store in short 2-3 characters database field.
        - Keep same replacements even if generating function is changed later.
- Only changed files are saved, both in Review and Cleanup processing modes.

# CSV Processing

CSV files can be configured to fill columns by template, even if no guarded words are found.

## CSV configuration per file type

```
csv:
  enabled: true
  files:
# CSV configuration per file type
# File path is matched with wildcards
    - filename: *accounts.csv
# Columns to replace with fill patterns
      columns:
        "name": "n{id}"       # fill by pattern
        "fullName": "fn{id}"  # {id} meas value from "id" column in same row 
        "email": ""           # clean
        "address": "NA"       # fill with static value
```

Values in not mentioned columns are processed and reported according to usual Text matching

## CSV in Report File

Report File combines results from CSV and Text processing

- Report File contains single row for configured CSV file.
    - **Context** column shows few examples of guarded words.
    - **Allow** column can be set, for example, you may set **Allow=true** for **Context=No guarded words found**.
- Columns not mentioned in CSV configuration are scanned as usual Text,
  and report contain line for each matched cell.

### Example scenario

**Given** words config

```
guard:
  values:
    "acme": ""
    "acm": ""
```

**And** CSV config

```
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

| Allow | Text                       | Context                    | File                        | Line | Replacement       | Comment                          |
|-------|----------------------------|----------------------------|-----------------------------|------|-------------------|----------------------------------|
|       | csv:name,fullName,mnemonic | Acme, Inc and 1 more match | resources/LegalEntity1.csv  | 1    | n{leId},fn{leId}, | CSV match 1 row replace all rows |
|       | acm                        | Code ACM.E                 | resources/LegalEntity1.csv  | 1    | w241563812        | Value acm                        |

# ZIP Processing

Files inside ZIP archives are scanned and replaced in same manner as plain files.

To compare ZIP files manually, for example, use **Ctrl+D** in Intellij IDEA.

# Tips and Tricks

- You can split review task in smaller parts by adding filters by module,
  e.g. ``**/mymodule/`` or by file type, e.g. ``**/*.csv``.
  Report format and file names in it will be same, so later you can remove those additional filters.
- You can restore original values in project code for some degree.
  Create other scan project with ``word-guard-value.csv`` content
  from ``generated-replacement.csv`` having "from" and "to" CSV columns swapped.
  Of course, this approach has some limitations, for example CSV content will not be restored. 