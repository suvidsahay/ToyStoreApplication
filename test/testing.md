# Testing Procedure:


### 1) Testing basic application functionality on AWS instance

#### a. Query request
![img.png](images/img.png)

#### b. Buy request
![img_1.png](images/img_1.png)


### 2) Entire application test
To run tests on the application you can run:

    ubuntu@ip-172-31-26-189:~/677/spring24-lab3-bhavikransubhe-suvidsahay/src/test/client$ javac ClientTest.java
    ubuntu@ip-172-31-26-189:~/677/spring24-lab3-bhavikransubhe-suvidsahay/src/test/client$ java ClientTest.java

Output

    TEST PASSED : Returned order numbers are equal to given order numbers.
    TEST PASSED: Successfully got product not found or out of stock(404 response code)

### 3) Simulating crash failures

1. Frontend service electing leader and re-electing leaders whenever current leader is dead
![img.png](images/img9.png)
![img_1.png](images/img10.png)

> Finally, simulate crash failures by killing a random order service replica while the clients is running, and then bring it back online after some time. Repeat this experiment several times and make sure that you test the case when the leader is killed. Can the clients notice the failures (either during order requests or the final order checking phase) or are they transparent to the clients?

No clients don't notice the failure because the system is failure transparent. Whenever the leader node has crashed, the frontend service contests a re-election 
and sends the request to the newly elected leader. 

> Do all the order service replicas end up with the same database file?

Yes as given below, the order service replicas sync when they restart and they end up with the same database file.


2. Order service 3 replicas

    Images showing data consistency across all replicas.

Replica 1
![img_5.png](images/img_5.png)

Replica 2
![img_7.png](images/img_7.png)

Replica 3
![img_6.png](images/img_6.png)

3. Catalog service updates

![img_8.png](images/img_8.png)