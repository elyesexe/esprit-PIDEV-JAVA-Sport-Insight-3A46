#!/bin/bash
# Script to run the Sport Insight Application (Linux/Mac)

echo "Compiling and running Sport Insight..."
echo ""

# Clean and compile
echo "Step 1: Cleaning and compiling..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Run the application using javafx-maven-plugin
echo ""
echo "Step 2: Running the application..."
mvn javafx:run

