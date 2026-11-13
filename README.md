> A recode of https://github.com/scales-dev/Simplify-Product-List
> because paying for Excel is an unnecessary cost.

## Usage

1. Download a Java 21 JRE, I personally recommend [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21)
2. Download a jar from [releases](https://github.com/scales-dev/Compress-Buying-List/releases)
3. Open your terminal, type "java -jar {drop jar} {drop input.csv}"
4. OUTPUT.csv will appear with the fixed product list!

## About
At work, each day the trade team has to manually simplify a long list of products. eg.

<img width="363" height="337" alt="image" src="https://github.com/user-attachments/assets/cd8716e6-0fcb-4743-9b3b-9b6c92939a51" />


A product can appear twice with slightly varied names, eg.

<img width="363" height="33" alt="image" src="https://github.com/user-attachments/assets/db53a6b2-f5e7-48e1-afd8-4734ab2064c6" />

And this project outputs an easier to read spreadsheet with the quantity and weights of the products. eg.

<img width="1183" height="368" alt="image" src="https://github.com/user-attachments/assets/0f33814a-08b9-411d-99ce-a1d7921f90dd" />

It tries its best to merge products that are named similar enough, thanks to fuzzywuzzy.
It adds up the totals of each product to save time adding up manually, and sorts alphabetically.