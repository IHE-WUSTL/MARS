INSERT INTO `mars`.`user` (`dba` ,`admin` ,`firstName` ,`id` ,`lastName` ,`pw`, `email`) VALUES (1, 1,  'Ralph',    'rmoult01', 'Moulton', 'gloria', 'moultonr@mir.wustl.edu');
INSERT INTO `mars`.`user` (`dba` ,`admin` ,`firstName` ,`id` ,`lastName` ,`pw`, `email`) VALUES (2, 1,  'Roger',    'rramje01', 'Ramjet',  'gloria', 'ramjectr@mir.wustl.edu');
INSERT INTO `mars`.`user` (`dba` ,`admin` ,`firstName` ,`id` ,`lastName` ,`pw`, `email`) VALUES (3, 0,  'Peggy',    'pflemi01', 'Fleming', 'gloria', 'flemingp@mir.wustl.edu');
INSERT INTO `mars`.`user` (`dba` ,`admin` ,`firstName` ,`id` ,`lastName` ,`pw`, `email`) VALUES (4, 0,  'Alexis',   'amoult01', 'Moulton', 'gloria', 'moultona@mir.wustl.edu');
INSERT INTO `mars`.`user` (`dba` ,`admin` ,`firstName` ,`id` ,`lastName` ,`pw`, `email`) VALUES (5, 1,  'Simeon',   'smoult01', 'Moulton', 'gloria', 'moultons@mir.wustl.edu');
INSERT INTO `mars`.`user` (`dba` ,`admin` ,`firstName` ,`id` ,`lastName` ,`pw`, `email`) VALUES (6, 1,  'Nicholas', 'nmoult01', 'Moulton', 'gloria', 'moultonn@mir.wustl.edu');
INSERT INTO `mars`.`user` (`dba` ,`admin` ,`firstName` ,`id` ,`lastName` ,`pw`, `email`) VALUES (7, 0,  'Peggy',    'pmarch01', 'March',   'gloria', 'marchp@mir.wustl.edu');
INSERT INTO `mars`.`user` (`dba` ,`admin` ,`firstName` ,`id` ,`lastName` ,`pw`, `email`) VALUES (8, 0,  'Christian','cmoult01', 'Moulton', 'gloria', 'moultonc@mir.wustl.edu');
INSERT INTO `mars`.`user` (`dba` ,`admin` ,`firstName` ,`id` ,`lastName` ,`pw`, `email`) VALUES (9, 1,  'Steve',    'smarti01', 'Martin',  'gloria', 'martins@mir.wustl.edu');
INSERT INTO `mars`.`user` (`dba` ,`admin` ,`firstName` ,`id` ,`lastName` ,`pw`, `email`) VALUES (10, 1, 'Matt',     'mhouse01', 'House',   'gloria', 'housem@mir.wustl.edu');
INSERT INTO `mars`.`user` (`dba` ,`admin` ,`firstName` ,`id` ,`lastName` ,`pw`, `email`) VALUES (11, 0, 'Steve',    'smoore01', 'Moore',   'gloria', 'moores@mir.wustl.edu');
INSERT INTO `mars`.`user` (`dba` ,`admin` ,`firstName` ,`id` ,`lastName` ,`pw`, `email`) VALUES (12, 0, 'Leslie',   'laerts01', 'Aerts',   'gloria', 'aertsl@mir.wustl.edu');
INSERT INTO `mars`.`study` (`dba`, `id`, `active`, `description`) VALUES(1,  'SITT',  1, 'Silent Infarct Transfusion Trial');
INSERT INTO `mars`.`study` (`dba`, `id`, `active`, `description`) VALUES(2,  'GOLD',  1, 'The effect of Gamma Rays on Man in the Moon Marygolds');
INSERT INTO `mars`.`study` (`dba`, `id`, `active`, `description`) VALUES(3,  'GRK',   0, 'Some Aspects of Contemporary Greek Orthodox Thought');
INSERT INTO `mars`.`study` (`dba`, `id`, `active`, `description`) VALUES(4,  'DUPA',  1, 'Comparative Study of the Effectiveness of CT Colonography and traditional Colonoscopy in early detection of Colon cancer');
INSERT INTO `mars`.`study` (`dba`, `id`, `active`, `description`) VALUES(5,  'SPINA', 1, 'Long term study of spina bifida corrective surgery. Prognosis and side effects');
INSERT INTO `mars`.`study` (`dba`, `id`, `active`, `description`) VALUES(6,  'ALIEN', 1, 'Genetic comparison of University of Illinois alumni with space aliens');
INSERT INTO `mars`.`study` (`dba`, `id`, `active`, `description`) VALUES(7,  'CONF',  0, 'Long term physical and psychiatric effects of frequent and sincere confessional practice in the Roman Catholic Church');
INSERT INTO `mars`.`study` (`dba`, `id`, `active`, `description`) VALUES(8,  'FRANK', 1, 'Feasibility study of multi-organ transplants including total or partial brain transplants');
INSERT INTO `mars`.`study` (`dba`, `id`, `active`, `description`) VALUES(9,  'OLD',   1, 'Correlation of aging and memory loss in Orthodox clergy');
INSERT INTO `mars`.`study` (`dba`, `id`, `active`, `description`) VALUES(10, 'HOOPS', 1, 'Estimates of productivity loss during NCAA basketball tournament');
INSERT INTO `mars`.`study` (`dba`, `id`, `active`, `description`) VALUES(11, 'CLUCK', 0, 'The effect of poultry consumption on language in young children residing in Vermont');
INSERT INTO `mars`.`study` (`dba`, `id`, `active`, `description`) VALUES(12, 'BAYER', 1, 'Usefullness of aspirin therapy in advanced brain cancer cases');
INSERT INTO `mars`.`studyUser` (`dba`, `studyDba`, `userDba`, `userRole`, `receiveAlerts`) VALUES(1, 1, 1, 2, 1);
INSERT INTO `mars`.`studyUser` (`dba`, `studyDba`, `userDba`, `userRole`, `receiveAlerts`) VALUES(2, 1, 4, 0, 1);
INSERT INTO `mars`.`studyUser` (`dba`, `studyDba`, `userDba`, `userRole`, `receiveAlerts`) VALUES(3, 2, 1, 1, 1);
INSERT INTO `mars`.`studyUser` (`dba`, `studyDba`, `userDba`, `userRole`, `receiveAlerts`) VALUES(4, 3, 1, 1, 0);
INSERT INTO `mars`.`studyUser` (`dba`, `studyDba`, `userDba`, `userRole`, `receiveAlerts`) VALUES(5, 4, 1, 0, 0);
INSERT INTO `mars`.`request` (`dba`, `dob`, `finalReportAlerts`, `lastName`, `firstName`, `mpi`, `scheduleAlerts`, `sex`, `studyDba`) VALUES (1, '1950-03-12', 1, 'MCSNEED', 'FARFLE', '12345', 1, 'M', 1);
INSERT INTO `mars`.`request` (`dba`, `dob`, `finalReportAlerts`, `lastName`, `firstName`, `mpi`, `scheduleAlerts`, `sex`, `studyDba`) VALUES (2, '1950-03-12', 1, 'MCSNEED', 'FARFLE', '12345', 1, 'M', 2);
INSERT INTO `mars`.`request` (`dba`, `dob`, `finalReportAlerts`, `lastName`, `firstName`, `mpi`, `scheduleAlerts`, `sex`, `studyDba`) VALUES (3, '1950-03-12', 1, 'MCSNEED', 'FARFLE', '12345', 1, 'M', 3);
INSERT INTO `mars`.`request` (`dba`, `dob`, `finalReportAlerts`, `lastName`, `firstName`, `mpi`, `scheduleAlerts`, `sex`, `studyDba`) VALUES (4, '1950-03-12', 1, 'MCSNEED', 'FARFLE', '12345', 1, 'M', 4);
INSERT INTO `mars`.`request` (`dba`, `dob`, `finalReportAlerts`, `lastName`, `firstName`, `mpi`, `scheduleAlerts`, `sex`, `studyDba`) VALUES (5, '1950-03-12', 1, 'MCSNEED', 'FARFLE', '12345', 1, 'M', 5);
INSERT INTO `mars`.`request` (`dba`, `dob`, `finalReportAlerts`, `lastName`, `firstName`, `mpi`, `scheduleAlerts`, `sex`, `studyDba`) VALUES (6, '1950-03-12', 1, 'MCSNEED', 'FARFLE', '12345', 1, 'M', 6);
INSERT INTO `mars`.`request` (`dba`, `dob`, `finalReportAlerts`, `lastName`, `firstName`, `mpi`, `scheduleAlerts`, `sex`, `studyDba`) VALUES (7, '1950-03-12', 1, 'MCSNEED', 'FARFLE', '12345', 1, 'M', 7);
INSERT INTO `mars`.`request` (`dba`, `dob`, `finalReportAlerts`, `lastName`, `firstName`, `mpi`, `scheduleAlerts`, `sex`, `studyDba`) VALUES (8, '1950-03-12', 1, 'MCSNEED', 'FARFLE', '12345', 1, 'M', 8);
INSERT INTO `mars`.`request` (`dba`, `dob`, `finalReportAlerts`, `lastName`, `firstName`, `mpi`, `scheduleAlerts`, `sex`, `studyDba`) VALUES (9, '1950-03-12', 1, 'MCSNEED', 'FARFLE', '12345', 1, 'M', 9);
INSERT INTO `mars`.`request` (`dba`, `dob`, `finalReportAlerts`, `lastName`, `firstName`, `mpi`, `scheduleAlerts`, `sex`, `studyDba`) VALUES (10, '1950-03-12', 1, 'MCSNEED', 'FARFLE', '12345', 1, 'M', 10);
INSERT INTO `mars`.`request` (`dba`, `dob`, `finalReportAlerts`, `lastName`, `firstName`, `mpi`, `scheduleAlerts`, `sex`, `studyDba`) VALUES (11, '1935-01-19', 1, 'MOORE', 'JACK', '6353809', 1, 'M', 1);
INSERT INTO `mars`.`alert` (`dba`, `firstName`, `lastName`, `mpi`, `dob`, `sex`, `event`, `arrivalTime`, `description`, `requestDba`) VALUES (1, 'FARFLE', 'MCSNEED', '12345', '1950-03-12', 'M', 0, '2011-03-04', 'SCHED MRI', 1);
INSERT INTO `mars`.`alert` (`dba`, `firstName`, `lastName`, `mpi`, `dob`, `sex`, `event`, `arrivalTime`, `description`, `requestDba`) VALUES (2, 'ANDREW', 'MCSNEED', '12345', '1950-03-12', 'M', 0, '2011-03-04', 'SCHED MRI', 1);
INSERT INTO `mars`.`alert` (`dba`, `firstName`, `lastName`, `mpi`, `dob`, `sex`, `event`, `arrivalTime`, `description`, `requestDba`) VALUES (3, 'FARFLE', 'MCSNEED', '11111', '1950-03-12', 'M', 0, '2011-03-05', 'CAN MRI', 1);
