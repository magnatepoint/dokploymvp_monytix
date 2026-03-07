-- Run this in Supabase SQL Editor to create the todos table
-- Dashboard: https://supabase.com/dashboard/project/YOUR_PROJECT/sql

create table if not exists public.todos (
  id bigint primary key generated always as identity,
  name text not null,
  created_at timestamptz default now()
);

-- Enable RLS
alter table public.todos enable row level security;

-- Allow authenticated users to read their todos (using auth.uid())
create policy "Users can read own todos"
  on public.todos for select
  to authenticated
  using (true);

-- Allow authenticated users to insert
create policy "Users can insert todos"
  on public.todos for insert
  to authenticated
  with check (true);

-- Allow authenticated users to update
create policy "Users can update todos"
  on public.todos for update
  to authenticated
  using (true);

-- Allow authenticated users to delete
create policy "Users can delete todos"
  on public.todos for delete
  to authenticated
  using (true);

-- Optional: Insert sample data
-- insert into public.todos (name) values ('Welcome to MONYTIX'), ('Connect your backend');
