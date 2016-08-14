
create table if not exists tags(
	id bigserial primary key, 
	tag varchar(255) not null
);

create table if not exists messages(
	id bigserial primary key,
	body text,
	title text,
	posted_by bigint
);

create table if not exists users(
	id bigserial primary key,
	username text,
	email text,
	passhash text
);

create table if not exists message_tags(
	message_id bigint,
	tag_id bigint,
	rating real
);

create table if not exists message_relations(
	id bigserial primary key,
	tag_id bigint,
	message_id bigint
);
