# code-egress
Scan and cleanup confidential details from code before publishing it.
Maintain consistent results among multiple sequential scans and pre-release scans.

## Features
- Matching on File name, Line, Word levels
- ZIP archives processing

## Process
- Scan to status file
- Manual review of status file to identify false-positives
- Actual Code modification

### Status file
CSV formatted file that can be viewed and edited in Excel, IDE plugin, or even simple text editor.
Convenient way is to put data as external link with manual update when required, for example like in 
[Inserting External Data](https://help.libreoffice.org/6.4/en-US/text/scalc/guide/webquery.html?DbPAR=CALC#bm_id3154346)

## Configuration

