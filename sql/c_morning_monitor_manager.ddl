CREATE TABLE bng000a.c_morning_monitor_manager
(
    EMPCD     char(7),
    EMPNM     varchar(20),
    EMPIP     VARCHAR(45),
    ACTIVE_YN char(1) DEFAULT '0'
);

insert into bng000a.c_morning_monitor_manager(EMPCD, EMPNM, EMPIP)
VALUES ('2360851', '배정모', '0:0:0:0:0:0:0:1')

;
update bng000a.c_morning_monitor_manager set ACTIVE_YN = '1' where EMPCD = '2360851';

--drop table bng000a.c_morning_monitor_manager;

select * from bng000a.c_morning_monitor_manager;