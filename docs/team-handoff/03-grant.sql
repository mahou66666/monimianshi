-- Run as local database administrator after creating both schemas.
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX ON interview_agent.* TO 'zzh'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX ON interview_studio_local.* TO 'zzh'@'%';
