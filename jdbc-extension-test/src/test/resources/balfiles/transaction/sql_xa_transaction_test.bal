// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
import ballerina/sql;
import ballerinax/jdbc;

type ResultCount record {
    int COUNTVAL;
};

function testXATransactonSuccess() returns (int, int) {
    jdbc:Client testDB1;
    jdbc:Client testDB2;
    testDB1 = new({
        url: "jdbc:h2:file:./target/H2_1/TestDB1",
        username: "SA",
        poolOptions: { maximumPoolSize: 1, isXA: true }
    });

    testDB2 = new({
        url: "jdbc:h2:file:./target/H2_2/TestDB2",
        username: "SA",
        poolOptions: { maximumPoolSize: 1, isXA: true }
    });

    transaction {
        sql:UpdateResult|error updateResult1 = testDB1->update("insert into Customers (customerId, name, creditLimit, country)
                                values (1, 'Anne', 1000, 'UK')");
        sql:UpdateResult|error updateResult2 = testDB2->update("insert into Salary (id, value ) values (1, 1000)");
    }

    int count1;
    int count2;
    //check whether update action is performed
    var dt1 = testDB1->select("Select COUNT(*) as countval from Customers where customerId = 1 ", ResultCount);
    count1 = getTableCountValColumn(dt1);

    var dt2 = testDB2->select("Select COUNT(*) as countval from Salary where id = 1", ResultCount);
    count2 = getTableCountValColumn(dt2);

    error? stopRet1 = testDB1.stop();
    error? stopRet2 = testDB2.stop();
    return (count1, count2);
}

function testXATransactonSuccessWithDataSource() returns (int, int) {
    jdbc:Client testDB1;
    jdbc:Client testDB2;
    testDB1 = new({
        url: "jdbc:h2:file:./target/H2_1/TestDB1",
        username: "SA",
        poolOptions: { maximumPoolSize: 1, isXA: true, dataSourceClassName: "org.h2.jdbcx.JdbcDataSource" }
    });

    testDB2 = new({
        url: "jdbc:h2:file:./target/H2_2/TestDB2",
        username: "SA",
        poolOptions: { maximumPoolSize: 1, isXA: true, dataSourceClassName: "org.h2.jdbcx.JdbcDataSource" }
    });

    transaction {
        sql:UpdateResult|error updateResult1 = testDB1->update("insert into Customers (customerId, name, creditLimit, country)
                                values (10, 'Anne', 1000, 'UK')");
        sql:UpdateResult|error updateResult2 = testDB2->update("insert into Salary (id, value ) values (10, 1000)");
    }

    int count1;
    int count2;
    //check whether update action is performed
    var dt1 = testDB1->select("Select COUNT(*) as countval from Customers where customerId = 10 ", ResultCount);
    count1 = getTableCountValColumn(dt1);

    var dt2 = testDB2->select("Select COUNT(*) as countval from Salary where id = 10", ResultCount);
    count2 = getTableCountValColumn(dt2);
    error? stopRet1 = testDB1.stop();
    error? stopRet2 = testDB2.stop();
    return (count1, count2);
}

function testXATransactonFailed1() returns (int, int) {
    jdbc:Client testDB1;
    jdbc:Client testDB2;
    testDB1 = new({
        url: "jdbc:h2:file:./target/H2_1/TestDB1",
        username: "SA",
        poolOptions: { maximumPoolSize: 1, isXA: true }
    });

    testDB2 = new({
        url: "jdbc:h2:file:./target/H2_2/TestDB2",
        username: "SA",
        poolOptions: { maximumPoolSize: 1, isXA: true }
    });

    error? ret = trap testXATransactonFailed1Helper(testDB1, testDB2);

    int count1;
    int count2;
    //check whether update action is performed
    var dt1 = testDB1->select("Select COUNT(*) as countval from Customers where customerId = 2", ResultCount);
    count1 = getTableCountValColumn(dt1);

    var dt2 = testDB2->select("Select COUNT(*) as countval from Salary where id = 2 ", ResultCount);
    count2 = getTableCountValColumn(dt2);

    error? stopRet1 = testDB1.stop();
    error? stopRet2 = testDB2.stop();
    return (count1, count2);
}

function testXATransactonFailed1Helper(jdbc:Client testDB1, jdbc:Client testDB2) {
    transaction {
        sql:UpdateResult|error updateResult1 = testDB1->update("insert into Customers (customerId, name, creditLimit, country)
                                    values (2, 'John', 1000, 'UK')");
        sql:UpdateResult|error updateResult2 = testDB2->update("insert into Salary (id, invalidColumn ) values (2, 1000)");
    }
}

function testXATransactonFailed2() returns (int, int) {
    jdbc:Client testDB1;
    jdbc:Client testDB2;
    testDB1 = new({
        url: "jdbc:h2:file:./target/H2_1/TestDB1",
        username: "SA",
        poolOptions: { maximumPoolSize: 1, isXA: true }
    });

    testDB2 = new({
        url: "jdbc:h2:file:./target/H2_2/TestDB2",
        username: "SA",
        poolOptions: { maximumPoolSize: 1, isXA: true }
    });
    error? ret = trap testXATransactonFailed2Helper(testDB1, testDB2);
    //check whether update action is performed
    var dt1 = testDB1->select("Select COUNT(*) as countval from Customers where customerId = 2", ResultCount);
    int count1 = getTableCountValColumn(dt1);

    var dt2 = testDB2->select("Select COUNT(*) as countval from Salary where id = 2 ", ResultCount);
    int count2 = getTableCountValColumn(dt2);

    error? stopRet1 = testDB1.stop();
    error? stopRet2 = testDB2.stop();
    return (count1, count2);
}

function testXATransactonFailed2Helper(jdbc:Client testDB1, jdbc:Client testDB2) {
    transaction {
        sql:UpdateResult|error updateResult1 = testDB1->update("insert into Customers (customerId, name, creditLimit, invalidColumn)
                                    values (2, 'John', 1000, 'UK')");
        sql:UpdateResult|error updateResult2 = testDB2->update("insert into Salary (id, value ) values (2, 1000)");
    }
}

function testXATransactonRetry() returns (int, int) {
    jdbc:Client testDB1;
    jdbc:Client testDB2;
    testDB1 = new({
        url: "jdbc:h2:file:./target/H2_1/TestDB1",
        username: "SA",
        poolOptions: { maximumPoolSize: 1, isXA: true }
    });

    testDB2 = new({
        url: "jdbc:h2:file:./target/H2_2/TestDB2",
        username: "SA",
        poolOptions: { maximumPoolSize: 1, isXA: true }
    });

    error? ret = trap testXATransactonRetryHelper(testDB1, testDB2);
    //check whether update action is performed
    var dt1 = testDB1->select("Select COUNT(*) as countval from Customers where customerId = 4",
        ResultCount);
    int count1 = getTableCountValColumn(dt1);

    var dt2 = testDB2->select("Select COUNT(*) as countval from Salary where id = 4", ResultCount);
    int count2 = getTableCountValColumn(dt2);

    error? stopRet1 = testDB1.stop();
    error? stopRet2 = testDB2.stop();
    return (count1, count2);
}

function testXATransactonRetryHelper(jdbc:Client testDB1, jdbc:Client testDB2) {
    int i = 0;
    transaction {
        if (i == 2) {
            sql:UpdateResult|error updateResult1 = testDB1->update("insert into Customers (customerId, name, creditLimit, country)
                        values (4, 'John', 1000, 'UK')");
        } else {
            sql:UpdateResult|error updateResult1 = testDB1->update("insert into Customers (customerId, name, creditLimit, invalidColumn)
                        values (4, 'John', 1000, 'UK')");
        }
        sql:UpdateResult|error updateResult2 = testDB2->update("insert into Salary (id, value ) values (4, 1000)");
    } onretry {
        i = i + 1;
    }
}

function getTableCountValColumn(table<ResultCount>|error result) returns int {
    int count = -1;
    if (result is table<ResultCount>) {
        while (result.hasNext()) {
            var rs = result.getNext();
            if (rs is ResultCount) {
                count = rs.COUNTVAL;
            }
        }
        return count;
    }
    return -1;
}
