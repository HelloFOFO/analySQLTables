This is a java project to analyze mysql script blocks and get table dependency sequentially.
Input is a script file and output is printed in the console.
Sample output is :
{1|TableA|TableB, 2|TableB|TableC}
This means: TableA depends on TableB while TableB depends on TableC.

