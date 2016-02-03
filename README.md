# Apache Jackrabbit Garbage Collector for Datastore configuration of repository

* Can be run as a standalone repository or clustered repository for online mark & sweep

* Examples can be found in examples/ directory

config.properties:
**repo** - path to repository home (repository.xml + workspaces)  
**gc_cron** - cron expression for Quartz Scheduler  
**gc_sweep** - run sweep? (true/false), will be run only then gc_mark = true  
**gc_mark** - run mark? (true/false)  

An empty file with actual mtime in datastore directory will be created before running gc_mark.
It can be used if your plan to clean old files in datastore manually (for example in bash scripts).

* Additional Info

Examples of a cron expression for Quartz: http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger

