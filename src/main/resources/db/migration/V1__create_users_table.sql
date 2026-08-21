create table users (
    id uuid not null,
    name varchar(120) not null,
    email varchar(320) not null,
    password_hash varchar(60) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,

    constraint pk_users primary key (id),
    constraint uk_users_email unique (email)
);