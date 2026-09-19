alter table categories
    add column icon varchar(30) not null default 'TAG';

alter table categories
    add constraint ck_categories_icon
        check (
            icon in (
                'TAG', 'HOME', 'FOOD', 'SHOPPING', 'TRANSPORT',
                'HEALTH', 'EDUCATION', 'LEISURE', 'BILLS', 'TRAVEL',
                'WORK', 'GIFT', 'PET', 'INVESTMENT', 'OTHER'
            )
        );
