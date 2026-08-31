create table categories (
    id uuid not null,
    user_id uuid not null,
    name varchar(120) not null,
    type varchar(20) not null,
    parent_category_id uuid,
    status varchar(20) not null default 'ACTIVE',
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,

    constraint pk_categories primary key (id),

    constraint fk_categories_user
        foreign key (user_id)
            references users(id)
            on delete cascade,

    constraint fk_categories_parent
        foreign key (parent_category_id)
            references categories(id)
            on delete restrict,

    constraint ck_categories_type
        check (type in ('INCOME', 'EXPENSE')),

    constraint ck_categories_status
        check (status in ('ACTIVE', 'INACTIVE'))
);

create index idx_categories_user_id
    on categories(user_id);

create index idx_categories_user_id_id
    on categories(user_id, id);

create index idx_categories_parent_id
    on categories(parent_category_id);