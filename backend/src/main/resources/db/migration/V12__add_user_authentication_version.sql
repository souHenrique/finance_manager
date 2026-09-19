alter table users
    add column authentication_version integer not null default 0;
