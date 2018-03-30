General Notes on IIGFeed. =================================

The IIGFeed NetBeans project is designed to process a single DFT feed from BJC
IIG and generate any information required by any research projects which need
this type of information.  It is envisioned that splits would be generated for
each project, filtered for needed DFT events, then reformatted into an output
feed or stored in a database, as needed by the particular application.

The inbound feed is an unfiltered DFT feed from IIG, sent to IP 10.39.42.204,
(which is mars-irat.wucon.wustl.edu) port 2020. As of this writing, the BJC
contact for this feed is Debbie Dussold BJC IIG, 314-362-7745. She seems to
know what she is doing; previous IIG people never seemed to be able to keep
track of the feed.

IIGFeed is run by this crontab entry:

1,16,31,46 * * * * /home/rmoult01/NetBeansProjects/IIGFeed/runDirectory/run.sh > /dev/null

The run.sh script in the runDirectory checks to see if the application is
already running by checking the port (2020) it uses, so it serves to restart
the application if it goes down for some reasong.  This has proved very stable.

HL7V2 processing is handled by hapi version 1.2, (hl7api.sourceforge.net).

IIGFeed looks for properties in the IIGFeed.ini file and the jog4j.properties
file, both in the runDirectory.

A two week rotating system log, which includes log messages from hapi and
hibernate is kept in System.log. A two week rotating log
of all received DFT messages is kept in Message.log.

Currently, the only research tool supported by IIGFeed is the MARS system, but
the MARS system supports a number of studies (14 as of this writing)

Notes on IIGFeed support of MARS system. ================================

MARS, an acronym whose exact meaning is a matter of debate (my choice: MIR
Alert Reporting System) is designed to save time for research study
coordinators by sending them email alerts when patients registered in their
studies have clinical radiology studies done at BJC. A MySQL database named
mars (see the MarsDB project for more details) is used by coordinators to
set up studies, assign research staff to studies, and enter Alert Requests for
patients registered in the study.

IIGFeed reads the incoming DFT records from the feed, looking for studies which
match to patiens in the mars database. When a match is found, IIGFeed inserts
an Alert into the mars db and sends a (PHI free) email to staff set up to
receive them, notifying them that they have new alert(s).

The mars database is accessed using Hibernate 3. Hibernate parameters are located
in the IIGFeed.ini file. A number of classes from the MarsDB project are used.

Emails are sent using the javax.mail package, wrapped in the Email class, also
in the MarsDB project. Email properties are in the IIGFeed.ini file.

A six month rotating log of generated alerts is maintained in Transaction.log.
