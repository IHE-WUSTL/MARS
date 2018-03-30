Notes on Schedule

The Schedule project is designed to run periodically as a batch job. When run,
it:

1. Queries the BJC Syngo RIS database via ODBC for a list of scheduled radiology
exams. This is done in a single query.

2. Queries the mars database (see the MarsDB project for detail) using Hibernate
for Alert Requests for active studies which have "Schedule alerts?" checked.

3. Matches the data in the two queries, generating an Excel spreadsheet with
one sheet per study, listing patients registered in that study with scheduled
exams.

4. Emails the completed spreadsheet to a predetermined list of coordinators.

Schedule is run by this crontab entry:

10 4 * * 1,3,5 /home/rmoult01/NetBeansProjects/Schedule/runDirectory/run.sh > /dev/null

The run.sh script in the runDirectory executes Schedule "headless", that is, as
a background job not attached to any graphic environment.

The most recent reports are kept in runDirectory/reports. A two week rolling
log is kept in runDirectory/logs.