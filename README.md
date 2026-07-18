# Venn Take Home Assignment
Candidate: Jabran Khan

## How to run the project
The project uses Java 25

For this project in particular, development was done using Amazon Corretto 25.0.3

This project accepts two command line arguments:
- an input file path
- an output file path

### IntelliJ
1. Open IntelliJ, import the project via the pom.xml file
2. Create/Edit the run configuration for `VennHomeTaskApplication`
    - Under `Modify options`, select `Program Arguments`
    - put your file path for in the input and output file
    - example: `input.txt output.txt` (in this example, the file will be read from the root of the directory and output file will be created in the root of the directory)
3. Run the application

### Terminal
1. Make sure the appropriate version of Java is installed in your terminal
   - use sdkman to keep track of multiple versions of Java
2. From the root of the directory, run the following command
   - `mvn spring-boot:run -Dspring-boot.run.arguments="{input_file_path} {output_file_path}"` 

## Assumptions Made
*Note: These assumptions were made to the best of my ability by carefully reading the given instructions and examining the given input and output files*

- I treated this as an actual production system, so I made design choices that I would consider "overengineered" for a take-home project
- If a load fund request comes with the same `id` and `customer_id`, this is ignored entirely (no entry in the output file)
- If a load is unsuccessful, it doesn't count to the daily limit of `3` loads

## Key Design Decisions

- Reading files
  - Files are read lazily using `Files.lines(inputPath)` ensuring we don't read the entire in memory
  - Used iterator to allow tracking of consumption metrics (lambdas would require atomic counters)
- Database
  - `load_fund` table - stores each fund that is being read from the input file
    - added a field called `accepted` which tracks if the fund was accepted or not
  - `customer_limit` - stores the limit values in buckets to keep track of daily and weekly limits
- Determining daily and weekly spend values
  - Created a `customer_limit` database to keep track of daily and weekly load balances in buckets
  - Each day a new fund is loaded, we create an entry in the `customer_limit` table which tracks the following:
    - How many funds were loaded that day
    - How much was loaded that day
    - How much was loaded that week
  - We scan the past week only once per day. Any additional loaded funds that day use the existing entry in the database
  - This makes reads more efficient with a tradeoff for some additional write complexity
  - **Alternative approach considered**:
    - Read through `load_fund` database and sum up all the transactions within the past day and past week everytime a new load attempt comes in
    - This is approach that would I normally go for in a take home project
    - Problem with this approach in a production system is that this requires scanning the table everytime a new fund comes it. Database reads can become expensive if we're dealing with millions of funds being loaded.
    - Works for smaller limits and less traffic. Becomes annoying if we want to expand to checking the past month or if we increase the daily fund count limit from 3 to unlimited.

## Verification and Testing

This project has a suite of unit tests and an integration test to verify that correctness of the application

The integration test runs directly against the H2 Database.

### How to run the tests
- IntelliJ
  - From the left hand navigation panel, right-click the `venn-home-task/scr/test/java` directory
  - Select `Run 'Run tests in 'Java''`
- Terminal
  - from the root of the directory, run `mvn test`