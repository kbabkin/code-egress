# code-egress

## Requirements
Scan and cleanup confidential details from code before publishing it.
Maintain consistent results among multiple sequential scans and pre-release scans.

## Features
- Matching on File name, Word levels
- todo: process statement/table-row level (SQL insert, CSV find-one-replace-all-columns)  
- todo: ZIP archives processing

## Process
- Scan to produce report file *code-report.csv*.
- Manual review of report file to fix configs and mark false-positives as **Allow=y**.
- Repeat scan and review steps as needed. 
- Backup report file *code-report.csv* or copy false-positives part of it to config *allow-report.csv*. 
- Cleanup Code according to collected configuration. Add JVM parameter **-Dwrite.inplace=true**

## Report file
Report contains violations of configured rules, one per line in CSV format.
File can be viewed and edited by IDE plugin, Excel, plain text editor.

Example report row:

| Allow | Text          | Context                | File                      | Line | Replacement              | Comment                                    |
|-------|---------------|------------------------|---------------------------|------|--------------------------|--------------------------------------------|
|       | 192.168.10.11 | check ip 192.168.10.11 | resources/sample-file.txt | 9    | h1163712847.domain.local | Pattern \d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3} |

### Report File Matching
Review and modify report file *code-report.csv* to mark false-positives or copy part of it to config *allow-report.csv* to use in next scans.
Report file *code-report.csv* is overridden after each scan.

How columns are used during scans and cleanup:
- **"Allow"** mark false-positive after manual review:
  - **"true"** - marks violation as false-positive to be excluded from replacement. Values "yes", "y", "1" are also accepted.
  - **"false"** - reviewed, true-positive, not allowed, will be replaced. Value is kept in next scan, used to differentiate reviewed and not reviewed lines.
  - empty value - not allowed, not reviewed, will be replaced.
- **Text** - found text
- **Context** - few characters before and after Text. Can be exact value from report or more narrow string.
- **File** - file path matching, wildcards allowed.
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

## Configuration
Check [src/main/resources/application.yml](src/main/resources/application.yml) and [scan-project/sample/config/scan-application.yml](scan-project/sample/config/scan-application.yml)

### Sample project 

- Edit [scan-project/sample/config/scan-application.yml](scan-project/sample/config/scan-application.yml)

```
read:
  # !!! Project folder to be scanned - provide actual value !!!
  folder: "/work/project/scanned-sample"
```

- Start **com.bt.code.egress.App**

Folder structure

```
scan-project                    # folder to collect scan configuration for mutliple repos
  sample                        # one repo/project configuration
    config                      # configuration to be persisted in Git among scans and releases
      scan-application.yml      # main configuration file
      word-guard-value.csv      # dictionary to find, optionally with replacement
      word-guard-pattern.csv    # patterns to find, optionally with replacement template
      allow-report.csv          # allowed false-positives, copied from report
    target                      # scan output files, ignored by Git and scan                        
      code-report.csv           # report file to manually identify false-positives
      generated-replacement.csv # can be adjusted and copied to dictionary
      preview                   # folder for replaced files to check during review phase
```

### Custom project

- Copy [scan-project/sample](scan-project/sample) , e.g. to *scan-project/myproject*
- Edit *read.folder* in *scan-project/myproject/config/scan-application.yml*
- Start **-Dscan.project=scan-project/myproject com.bt.code.egress.App**

### Test configuration
Can be used to check how it works, verify configuration parts, reproduce bugs, etc.

- Start **com.bt.code.egress.TestApp**

Test folders:
 
- src/test/resources/config - configuration.
- src/test/resources/testdata - test files - TXT, CSV, ZIP, etc.
- target/scan-project/target - output files.