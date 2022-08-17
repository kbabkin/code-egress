# code-egress

## Requirements
Scan and cleanup confidential details from code before publishing it.
Maintain consistent results among multiple sequential scans and pre-release scans.

## Features
- Matching on File name, Line, Word levels
- todo: ZIP archives processing

## Process
- Scan to produce report file.
- Manual review of report file to fix configuration and mark false-positives.
- Repeat scan and review steps as needed. 
- Backup report file or copy needed part of it to config. 
- Cleanup Code according to collected configuration.

## Report file
CSV formatted file that can be viewed and edited by IDE plugin, Excel, plain text editor.
Set "Allow=true" to mark lines as false-positives for next scan ("yes", "y", "1" values are also accepted).

## Configuration

