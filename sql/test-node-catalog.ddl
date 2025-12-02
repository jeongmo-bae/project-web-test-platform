show
    databases;

create
    database project_testauto;

show
    tables from project_testauto;

create table project_testauto.C_TEST_NODE_CATALOG
(
    unique_id        varchar(200) not null primary key,
    parent_unique_id varchar(200),
    displayname      varchar(200),
    classname        varchar(200),
    type             varchar(20),
    updatedat        timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)
;
# drop table project_testauto.C_TEST_NODE_CATALOG;

select * from project_testauto.C_TEST_NODE_CATALOG
where unique_id = '[engine:junit-jupiter]';