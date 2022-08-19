# code-egress

## Requirements
Scan and cleanup confidential details from code before publishing it.
Maintain consistent results among multiple sequential scans and pre-release scans.

## Features
- Matching on File name, Word levels
- todo: process statement/table-row level (SQL insert, CSV find-one-replace-all-columns)  
- todo: ZIP archives processing

## Process
- Scan to produce report file.
- Manual review of report file to fix configuration and mark false-positives.
- Repeat scan and review steps as needed. 
- Backup report file or copy false-positives part of it to config. 
- Cleanup Code according to collected configuration.

## Report file
Report contains violations of configured rules, one per line in CSV format.
File can be viewed and edited by IDE plugin, Excel, plain text editor.

Set colum **"Allow"** value to mark violation for following scans and cleanup:
- **"true"** - marks violation as false-positive to be excluded from replacement. Values "yes", "y", "1" are also accepted.
- **"false"** - reviewed, true-positive, not allowed, will be replaced. Value is kept in next scan, used to differentiate reviewed and not reviewed lines.
- empty value - not allowed, not reviewed, will be replaced.

## Configuration
**TBD** see application.yml
