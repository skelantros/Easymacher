drop table if exists groups_to_words;
drop table if exists word_groups;

drop table if exists words_nouns;
drop type if exists gender;

drop table if exists words_base;

drop table if exists users;

create table users(
	user_id serial primary key,
	auth0_sub varchar(256) not null unique,
	username varchar(128) not null,
	first_name varchar(32),
	last_name varchar(32),
	is_admin boolean not null
);

create table words_base(
	word_id serial primary key,
	user_id integer not null references users on delete cascade,
	word varchar(256) not null,
	w_translate varchar(256),
	w_info varchar(256),
	has_type boolean not null
);

create type gender as enum('m', 'f', 'n');

create table words_nouns(
	noun_id serial primary key,
	word_id integer not null references words_base on delete cascade,
	plural varchar(256),
	n_gender gender not null
);

create table word_groups(
	group_id serial primary key,
	user_id integer not null references users on delete cascade,
	g_name varchar(256) not null,
	is_shared boolean not null
);

create table groups_to_words(
	group_id integer not null references word_groups on delete cascade,
	word_id integer not null references words_base on delete cascade,
	unique (group_id, word_id)
);