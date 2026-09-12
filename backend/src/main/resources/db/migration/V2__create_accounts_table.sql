create table accounts (
      id uuid not null,
      user_id uuid not null,
      name varchar(120) not null,
      type varchar(30) not null,
      institution varchar(160),
      initial_balance numeric(19, 2) not null,
      current_balance numeric(19, 2) not null,
      status varchar(30) not null,
      version bigint not null default 0,
      created_at timestamp with time zone not null,
      updated_at timestamp with time zone not null,

      constraint pk_accounts primary key (id),

      constraint fk_accounts_user
          foreign key (user_id)
              references users(id)
              on delete cascade,

      constraint ck_accounts_type
          check (
              type in (
                       'CHECKING',
                       'SAVINGS',
                       'WALLET',
                       'DIGITAL_ACCOUNT',
                       'OTHER'
                  )
              )
);

create index idx_accounts_user_id
    on accounts(user_id);

create index idx_accounts_user_id_id
    on accounts(user_id, id);