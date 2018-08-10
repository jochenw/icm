-- @IcmChange(name="Create Table", type="sql", version="0.0.1", description="Creates the test table")
-- @Attribute(name="prefix", value="jdbc")

DROP TABLE IF EXISTS FOO;
CREATE TABLE FOO (
  id BIGINT NOT NULL PRIMARY KEY,
  name VARCHAR(20) NOT NULL,
  description VARCHAR(80) NOT NULL
);
