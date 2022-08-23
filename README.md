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
- Cleanup Code according to collected configuration.

## Report file
Report contains violations of configured rules, one per line in CSV format.
File can be viewed and edited by IDE plugin, Excel, plain text editor.

Set colum **"Allow"** value to mark violation for following scans and cleanup:
- **"true"** - marks violation as false-positive to be excluded from replacement. Values "yes", "y", "1" are also accepted.
- **"false"** - reviewed, true-positive, not allowed, will be replaced. Value is kept in next scan, used to differentiate reviewed and not reviewed lines.
- empty value - not allowed, not reviewed, will be replaced.

Example row:

| Allow | Text | Context | File | Line | Replacement | Comment                 |
|-------|------|---------|------|------|-------------|-------------------------|
|       | 192.169.10.11 | check ip 192.169.10.11 | src/test/resources/sample-file.txt | 9 | h1163712847.domain.local | Pattern \d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3} |


## Configuration
Check *src/main/resources/application.yml* and *scan-project/sample/config/scan-application.yml*

### Sample project 

- Edit *scan-project/sample/config/scan-application.yml*


    read:
      # !!! Project folder to be scanned - provide actual value !!!
      folder: "/work/project/scanned-sample"


- Start **com.bt.code.egress.App**

Folder structure


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


### Custom project

- Copy *scan-project/sample* , e.g. to *scan-project/myproject*
- Edit *read.folder* in *scan-project/myproject/config/scan-application.yml*
- Start **-Dscan.project=scan-project/myproject com.bt.code.egress.App**

### Test congiguration

- Start **com.bt.code.egress.TestApp**
