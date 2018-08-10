# icm
## The Incremental Change Manager

ICM (the Incremental Change Manager) is a tool, which allows to ensure, that a system is configured properly by applying a set of small, incremental changes, in a given order. For example, in the case of an SQL database, you might have several scripts, each of which creates a table, a database user, a constraint, or imports a piece of data.

The changes are given by a set of files, one for any change.

ICM follows several ideas, as given by [Flyway](https://flywaydb.org/), except that ICM aims to extend those ideas beyond the scope of SQL databases. For example, ICM should allow you to create a JMS queue, a JNDI entry, or similar resources in quite the same manner, by defining the creation of those resources in a change file, as long as a suitable ICM plugin is available. As of this writing, there are plugins for

  - executing SQL scripts
  - creating objects in ActiveMQ
  - executing a Groovy script
  - executing shell commands
  - editing text files

More will be available over times, as more plugins will be added.

