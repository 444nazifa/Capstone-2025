-- Migration:Create reminders table
-- Author: Mohamed Ali
-- Description: it adds a new table to store user created reminders, such as like “take Ibuprofen at 9:00 AM”. 
-- Date: 2025-11-03


create table if not exists reminders 
(
  id uuid primary key default uuid_generate_v4(), -- every reminder gets its own unique id
  user_id uuid references auth.users (id) on delete cascade,
  title text not null,
  description text,
  reminder_time timestamptz not null,
  is_completed boolean default false,
  created_at timestamptz default now()
);
