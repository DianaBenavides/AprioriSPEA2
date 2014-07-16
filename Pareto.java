package analysisManager.pareto;

import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import analysisManager.weka.associations.LabeledItemSet;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

public class Pareto {

	int archiveSize;
	int numberOfGenerations;
	
	List<String[]> dominatedChromosomes;
	List<String[]> nonDominatedChromosomes;
	List<String[]> dominatingChromosomes;

	Map<String, Integer> dominatedChromosomesNumber;
	Map<String, Integer> dominatingChromosomesNumber;

	Map <String, Double> dominatedChromosomesFitness;
	Map <String, Double> nonDominatedChromosomesFitness;
	
	Map <String, Double> dominatedChromosomesRawFitness;
	Map <String, Double> nonDominatedChromosomesRawFitness;
	
	int dominatedPopulationSize;
	int nonDominatedPopulationSize;

	public Pareto()
	{
		archiveSize=10;
		numberOfGenerations=5;
		
		dominatedChromosomes=new ArrayList<String[]>();
		nonDominatedChromosomes=new ArrayList<String[]>();
		dominatingChromosomes=new ArrayList<String[]>();
		dominatedChromosomesNumber=new HashMap<String, Integer>();
		dominatingChromosomesNumber=new HashMap<String, Integer>();
	
		dominatedChromosomesFitness=new HashMap<String, Double>();
		nonDominatedChromosomesFitness=new HashMap<String, Double>();
		
		dominatedChromosomesRawFitness=new HashMap<String, Double>();
		nonDominatedChromosomesRawFitness=new HashMap<String, Double>();
	}
	
	public void pareto(int currentIteration, int numberOfMeasures)
	{
		List<String[]> population=new ArrayList<String[]>();
		int generation=0;
		initFiles(currentIteration);
	
		try 
		{
			while(generation<numberOfGenerations)
			{
				CsvReader readerGeneration=new CsvReader("currentGeneration.csv");
				
				//Initialize everything
				dominatedChromosomes=new ArrayList<String[]>();
				nonDominatedChromosomes=new ArrayList<String[]>();
				dominatingChromosomes=new ArrayList<String[]>();
				dominatedChromosomesNumber=new HashMap<String, Integer>();
				dominatingChromosomesNumber=new HashMap<String, Integer>();
			
				dominatedChromosomesFitness=new HashMap<String, Double>();
				nonDominatedChromosomesFitness=new HashMap<String, Double>();
				
				dominatedChromosomesRawFitness=new HashMap<String, Double>();
				nonDominatedChromosomesRawFitness=new HashMap<String, Double>();

				//If first generation
				if(!readerGeneration.readRecord())
				{
					CsvReader reader=new CsvReader("file-ItemsetsMeasures-Iteration"+currentIteration+".csv");
					population=paretoFrontier(currentIteration, numberOfMeasures, reader, generation);
					writePopulation(population, generation);
					reader.close();
				}
				
				else 
				{
					if(population.size()>0)
					{
						population=paretoFrontier(currentIteration, numberOfMeasures, readerGeneration, generation);
						writePopulation(population, generation);
					}
				}

				//If there is still dominated chromosomes
				if(dominatedPopulationSize>0){
					generation++;
					readerGeneration.close();
				}
				
				else
				{
					readerGeneration.close();
					break;
				}
			}
			
			writeParetoFrontier("currentGeneration.csv", currentIteration);
		}
		
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void initFiles(int currentIteration)
	{
		try
		{
			//Generation file
			File generationFile=new File("file-dominatedItemsets-"+currentIteration+".csv");
			generationFile.delete();
			
			CsvWriter newGenerationFile=new CsvWriter(new FileWriter("currentGeneration.csv", false), ',');
			newGenerationFile.close();
			
			//Other files
			File dominatedItemSets=new File("file-dominatedItemsets-"+currentIteration+".csv");
			dominatedItemSets.delete();
			
			CsvWriter newDominatedItemSets=new CsvWriter(new FileWriter("file-dominatedItemsets-"+currentIteration+".csv", false), ',');
			newDominatedItemSets.close();
			
			File dominatedItemSetsFitness=new File("file-dominatedItemsetsFitness-"+currentIteration+".csv");
			dominatedItemSetsFitness.delete();
			
			CsvWriter newDominatedItemSetsFitness=new CsvWriter(new FileWriter("file-dominatedItemsetsFitness-"+currentIteration+".csv", false), ',');
			newDominatedItemSetsFitness.close();
			
			File dominatingDominatedItemsets=new File("file-dominatingDominatedItemsets-"+currentIteration+".csv");
			dominatingDominatedItemsets.delete();
			
			CsvWriter newDominatingDominatedItemsets=new CsvWriter(new FileWriter("file-dominatingDominatedItemsets-"+currentIteration+".csv", false), ',');
			newDominatingDominatedItemsets.close();
			
			File dominatingItemsets=new File("file-dominatingItemsets-"+currentIteration+".csv");
			dominatingItemsets.delete();
			
			CsvWriter newDominatingItemsets=new CsvWriter(new FileWriter("file-dominatingItemsets-"+currentIteration+".csv", false), ',');
			newDominatingItemsets.close();
			
			File nonDominatingItemsets=new File("file-nonDominatingItemsets-"+currentIteration+".csv");
			nonDominatingItemsets.delete();
			
			CsvWriter newNonDominatingItemsets=new CsvWriter(new FileWriter("file-nonDominatingItemsets-"+currentIteration+".csv", false), ',');
			newNonDominatingItemsets.close();
			
			File nonDominatingItemsetsFitness=new File("file-nonDominatingItemsetsFitness-"+currentIteration+".csv");
			nonDominatingItemsetsFitness.delete();
			
			CsvWriter newNonDominatingItemsetsFitness=new CsvWriter(new FileWriter("file-nonDominatingItemsetsFitness-"+currentIteration+".csv", false), ',');
			newNonDominatingItemsetsFitness.close();
		}

		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
	}
	//Determina frontera Pareto, haciendo llamados a otros métodos
	public List<String[]> paretoFrontier(int currentIteration, int numberOfMeasures, CsvReader reader, int generation)
	{
		List<String[]> population=new ArrayList<String[]>();
		String currentChrom="";
		String[] currentChromosome=null;
		int recordNumber=0;
		
		try 
		{
			while(reader.readRecord())
			{
				if(generation==0)
				{
					if(recordNumber!=0)
					{
						currentChromosome=new String[numberOfMeasures+1];
						for(int i=0;i<=numberOfMeasures;i++)
						{
							currentChromosome[i]=reader.get(i);
						}
						
						population.add(currentChromosome);
					}
					recordNumber++;
				}
					
				else
				{
					
					currentChromosome=new String[numberOfMeasures+1];
					for(int i=1;i<=(numberOfMeasures+1);i++)
					{
						currentChrom=reader.get(0);
							
						if(currentChrom.split(",")[0].equals(String.valueOf(generation-1)))
						{
							currentChromosome[i-1]=reader.get(i);
						}
							
						else
						{
							currentChromosome[i-1]=reader.get(i);
						}
					}
					population.add(currentChromosome);
				}
			}
	
			if(population.size()>1)
			{
				//STEP 1: Resume population
				if(generation==0)
				{
					if(population.size()>50)
					{
						population=initialPopulation(population);
					}
				}
				
				//STEP 2: Find dominated and non-dominated
				getDominance(population, currentIteration, generation);

				//STEP 2: Get populations size
				nonDominatedPopulationSize=nonDominatedChromosomes.size();
				dominatedPopulationSize=dominatedChromosomes.size();
				
				//If there is still dominated population
				if(nonDominatedPopulationSize>0 && dominatedPopulationSize>0)
				{
					//STEP 2: Calculate fitness
					calculateFitness(numberOfMeasures, currentIteration, generation);
					
					//STEP 3: Make environmental selection
					population=environmentalSelection();
					
					//STEP 5: Perform binary tournament selection
					population=performBinaryTournamentSelection(population);
	
					//STEP 6: Perform mutation and crossover over the mating pool (binary tournament selection)
					population=performMutationAndCrossover(population);
				}
			}
		}
		
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		return population;
	}
	
	//Get initial population (P0), by randomly selecting some population 
	//For now, population size = original population size / 2
	public List<String[]> initialPopulation(List<String[]> completePopulation)
	{
		List<String[]> initialPopulation=new ArrayList<String[]>();
		int populationSize=completePopulation.size() / 2;
		int[] randomNumbers=new int[populationSize];
		Random random=new Random();
		HashSet usedNumbers=new HashSet();
		int number=0;
		
		for(int i=0;i<randomNumbers.length;i++)
		{
			do
			{
				number=random.nextInt(randomNumbers.length);
			}while(!usedNumbers.add(number));
			
			randomNumbers[i]=number;
		}

		for(int i=0;i<randomNumbers.length;i++)
		{
			initialPopulation.add(completePopulation.get(randomNumbers[i]));
		}

		return initialPopulation;
	}
	
	//Identify dominant, dominated and non-dominated chromosomes
	public List<String[]> getDominance(List<String[]> population, int currentIteration, int currentGeneration)
	{
		int isDominated=0;
		int isDominating=0;
		
		for(int i=0;i<population.size();i++)
		{
			isDominated=isDominated(population, population.get(i));
			
			//Write to output file only dominated chromosomes
			if(isDominated>0)
			{
				dominatedChromosomes.add(population.get(i));
				dominatedChromosomesNumber.put(population.get(i)[0], isDominated);
				
				if(i==0)
				{
					writeDominatedChromosomes(population.get(i), isDominated, currentIteration, true);
				}
				
				else
				{
					writeDominatedChromosomes(population.get(i), isDominated, currentIteration, false);
				}
			}
			
			//Write non-dominated chromosomes
			else 
			{
				nonDominatedChromosomes.add(population.get(i));
				
				if(i==0)
				{
					writeNonDominatedChromosomes(population.get(i), currentIteration, true);
				}
				
				else
				{
					writeNonDominatedChromosomes(population.get(i), currentIteration, false);
				}
			}
		}

		for(int i=0;i<population.size();i++)
		{
			isDominating=isDominating(population, population.get(i), currentGeneration);
		
			//Write to output file all chromosomes, with the  number of itemsets it dominates
			dominatingChromosomes.add(population.get(i));
			dominatingChromosomesNumber.put(population.get(i)[0], isDominating);
			
			if(i==0)
			{
				writeDominatingChromosomes(population.get(i), isDominating, currentIteration, true);
			}
			
			else
			{
				writeDominatingChromosomes(population.get(i), isDominating, currentIteration, false);
			}
		}

		return null;
	}
	
	//Checks whether chromosome is dominated, and by which ones
	public int isDominated(List<String[]> population, String[]chromosome)
	{
		boolean dominated=false;
		boolean dominated2=false;
		int dominatedBy=0;
		String[] dominatingChromosome=null;
		
		for(int i=0;i<population.size();i++)
		{
			dominatingChromosome=population.get(i);

			dominated=true;
			dominated2=false;
			for(int j=1;j<dominatingChromosome.length;j++)
			{
				if(!(dominatingChromosome.equals(chromosome)))
				{
					//Except for the first measure (support), in which case the higher the best
					if(j!=1)
					{
						//If not garbage measures
						if(Double.parseDouble(dominatingChromosome[j])!=0.0 && Double.parseDouble(chromosome[j])!=0.0)
						{
							if(!(Double.parseDouble(dominatingChromosome[j])<=(Double.parseDouble(chromosome[j]))))
							{
								dominated=false;
							}
							
							if((Double.parseDouble(dominatingChromosome[j]))<(Double.parseDouble(chromosome[j])))
							{
								dominated2=true;
							}
						}
					}
					
					else 
					{
						if(!(Double.parseDouble(dominatingChromosome[j])>=(Double.parseDouble(chromosome[j]))))
						{
							dominated=false;
						}
						
						if((Double.parseDouble(dominatingChromosome[j]))>(Double.parseDouble(chromosome[j])))
						{
							dominated2=true;
						}
					}
				}
			}
			
			if(dominated && dominated2)
			{
				dominatedBy++;
			}
		}
		
		return dominatedBy;
	}
	
	//Checks whether chromosome is dominating, and which ones it dominates
	public int isDominating(List<String[]> population, String[]chromosome, int currentGeneration)
	{
		boolean dominating=false;
		boolean dominating2=false;
		int dominates=0;
		String[] dominatedChromosome=null;
		
		for(int i=0;i<population.size();i++)
		{
			dominatedChromosome=population.get(i);

			dominating=true;
			dominating2=false;
			
			for(int j=1;j<dominatedChromosome.length;j++)
			{
				if(!(chromosome.equals(dominatedChromosome)))
				{
					//Except for the first measure (support), in which case the higher the best

					if(j!=1)
					{
						if(Double.parseDouble(dominatedChromosome[j])!=0.0 && Double.parseDouble(chromosome[j])!=0.0)
						{
							if(!((Double.parseDouble(chromosome[j]))<=(Double.parseDouble(dominatedChromosome[j]))))
							{
								dominating=false;
							}
							
							if((Double.parseDouble(chromosome[j]))<(Double.parseDouble(dominatedChromosome[j])))
							{
								dominating2=true;
							}
						}
					}
					
					else 
					{
						if(!((Double.parseDouble(chromosome[j]))>=(Double.parseDouble(dominatedChromosome[j]))))
						{
							dominating=false;
						}
						
						if((Double.parseDouble(chromosome[j]))>(Double.parseDouble(dominatedChromosome[j])))
						{
							dominating2=true;
						}						
					}
				}
			}
			
			if(dominating && dominating2)
			{
				if(i==0)
				{
					writeDominatingDominatedChromosomes(chromosome, dominatedChromosome, true);
				}
				
				else
				{
					writeDominatingDominatedChromosomes(chromosome, dominatedChromosome, false);
				}
				dominates++;
			}
		}
		
		return dominates;
	}
	
	//Calculate fitness and raw fitness R(i) + D(i), according with SPEA2, pág. 7
	public void calculateFitness(int numberOfMeasures, int currentIteration, int currentGeneration)
	{
		//Use pareto superiority, inferiority and non-dominance concepts
		//Number of dominating and dominated solutions are taken into account
		//Use formula from page 7 SPEA2
		
		//STEP 1
		//First, calculate fitness without having into account density
		try 
		{
			String dominatingChromosome=new String();
			String dominatedChromosome=new String();
			String dominatedChromosomeValue=new String();
			int j=0;
			
			//For non-dominated chromosomes, fitness=0
			for(int i=0;i<nonDominatedChromosomes.size();i++)
			{
				nonDominatedChromosomesFitness.put(nonDominatedChromosomes.get(i)[0], 0.0);
			}			
			
			//For dominated chromosomes, calculate fitness}
			CsvReader reader=new CsvReader("file-dominatingDominatedItemsets.csv");
			while(reader.readRecord())
			{
				for(int i=0;i<=numberOfMeasures;i++)
				{
					dominatingChromosome=reader.get(i);
					break;
				}
				
				j=0;
				for(int i=(numberOfMeasures+1);i<=((numberOfMeasures*2)+1);i++)
				{
					dominatedChromosome=reader.get(i);
					dominatedChromosomeValue=reader.get(i+1);
					break;
				}

				if(dominatingChromosomesNumber.containsKey(dominatingChromosome))
				{
					if(!dominatedChromosomesFitness.containsKey(dominatedChromosome))
					{
						dominatedChromosomesFitness.put(dominatedChromosome, dominatingChromosomesNumber.get(dominatingChromosome).doubleValue());
					}
					
					else
					{
						Double value=dominatedChromosomesFitness.get(dominatedChromosome).doubleValue();
						dominatedChromosomesFitness.remove(dominatedChromosome);
						
						value=value+dominatingChromosomesNumber.get(dominatingChromosome).intValue();
						dominatedChromosomesFitness.put(dominatedChromosome, value);
					}
				}
			}
			
			reader.close();
		}
		
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		//STEP 2
		//Now, calculate density based on density information (may be useful for non-dominated individuals, which fitness=0)
		//According with page 7 of SPEA2

		//For non-dominated chromosomes
		for(int i=0;i<nonDominatedChromosomes.size();i++)
		{
			double densityFitness=calculateChromosomeDensity(nonDominatedChromosomes.get(i));
			double singleFitness=0.0;
			double rawFitness=0.0;
			
			for(Map.Entry entry: nonDominatedChromosomesFitness.entrySet())
			{
				String key=(String) entry.getKey();
				singleFitness=(Double) entry.getValue();
				
				if(key.equals(nonDominatedChromosomes.get(i)[0]))
				{
					rawFitness=singleFitness+densityFitness;

					//Calculate total fitness
					nonDominatedChromosomesRawFitness.put(nonDominatedChromosomes.get(i)[0], rawFitness);
					
					if(i==0)
					{
						writeNonDominatedFitness(nonDominatedChromosomes.get(i)[0], rawFitness, currentIteration, true, currentGeneration);
					}
					
					else 
					{
						writeNonDominatedFitness(nonDominatedChromosomes.get(i)[0], rawFitness, currentIteration, false, currentGeneration);
					}
					break;
				}
			}
		}
		
		//For dominated chromosomes
		for(int i=0;i<dominatedChromosomes.size();i++)
		{
			double densityFitness=calculateChromosomeDensity(dominatedChromosomes.get(i));
			double singleFitness=0.0;
			double rawFitness=0.0;
			
			for(Map.Entry entry: dominatedChromosomesFitness.entrySet())
			{
				String key=(String) entry.getKey();
				singleFitness=(Double) entry.getValue();
				
				if(key.equals(dominatedChromosomes.get(i)[0]))
				{
					rawFitness=singleFitness+densityFitness;

					//Calculate total fitness
					dominatedChromosomesRawFitness.put(dominatedChromosomes.get(i)[0], rawFitness);
					
					if(i==0)
					{
						writeDominatedFitness(dominatedChromosomes.get(i)[0], rawFitness, currentIteration, true, currentGeneration);
					}
					
					else
					{
						writeDominatedFitness(dominatedChromosomes.get(i)[0], rawFitness, currentIteration, false, currentGeneration);
					}
					break;
				}
			}
		}
	}

	//Calculate chromosome density; may be used for calculating raw fitness
	public double calculateChromosomeDensity(String[] chromosome)
	{
		//According with SPEA2, pag. 7
		double chromosomeDensity=0.0;
		double n=(double)nonDominatedPopulationSize + (double)dominatedPopulationSize;
		List<Double> chromosomeDistances=calculateChromosomeDistances(chromosome);
		Collections.sort(chromosomeDistances);
		
		n=(double)nonDominatedPopulationSize + (double)dominatedPopulationSize;
		double k=Math.sqrt(n);
		
		if(chromosomeDistances.size()>=((int)k))
		{
			double selected=chromosomeDistances.get((int)k);
			chromosomeDensity=1/(selected+2);
		}
		
		return chromosomeDensity;
	}
	
	//Calculate some chromosome distances with respect to all other chromosomes
	public List<Double> calculateChromosomeDistances(String[] chromosome)
	{
		List<Double> chromosomeDistances=new ArrayList<Double>();
		Double[] chromosomeDifferencesSquare=new Double[chromosome.length-1];
		Double chromosomeDifferencesSum=0.0;
		Double chromosomeDistance=0.0;
		
		//Extract distances with respect to dominated chromosomes
		for(int i=0;i<dominatedChromosomes.size();i++)
		{
			if(!chromosome.equals(dominatedChromosomes.get(i)))
			{
				for(int j=1;j<chromosome.length;j++)
				{
					chromosomeDifferencesSquare[j-1]=(Math.pow(Double.parseDouble(chromosome[j]) - Double.parseDouble(dominatedChromosomes.get(i)[j]),2));
				}
				
				chromosomeDifferencesSum=0.0;
				for(int j=0;j<chromosomeDifferencesSquare.length;j++)
				{
					chromosomeDifferencesSum=chromosomeDifferencesSum+chromosomeDifferencesSquare[j];
				}
				
				chromosomeDistance=Math.sqrt(chromosomeDifferencesSum);
				chromosomeDistances.add(chromosomeDistance);
			}
		}

		//Extract distances with respect to non-dominated chromosomes
		for(int i=0;i<nonDominatedChromosomes.size();i++)
		{
			if(!chromosome.equals(nonDominatedChromosomes.get(i)))
			{
				for(int j=1;j<chromosome.length;j++)
				{
					chromosomeDifferencesSquare[j-1]=(Math.pow(Double.parseDouble(chromosome[j]) - Double.parseDouble(nonDominatedChromosomes.get(i)[j]),2));
				}
				
				chromosomeDifferencesSum=0.0;
				for(int j=0;j<chromosomeDifferencesSquare.length;j++)
				{
					chromosomeDifferencesSum=chromosomeDifferencesSum+chromosomeDifferencesSquare[j];
				}
				
				chromosomeDistance=Math.sqrt(chromosomeDifferencesSum);
				chromosomeDistances.add(chromosomeDistance);
			}
		}
		
		return chromosomeDistances;
	}
	
	//Environmental selection - STEP 3: Pi+1 = |P (no dominados) + P
	public List<String[]> environmentalSelection()
	{
		List<String[]> nextPopulation=new ArrayList<String[]>();
		
		//Añada todos los no-dominados a la próxima generación
		nextPopulation.addAll(nonDominatedChromosomes);
		
		//Si los no-dominados encontrados son mayores al tamaño del archivo, aplicar el método de trunk
		if(nextPopulation.size()>archiveSize)
		{
			//THIS IS COSTLY!
			//While nextPopulationSize bigger than archiveSize, remove one by one the least distance chromosome with respect to some other
			
			while(nextPopulation.size()>archiveSize)
			{
				Map<String, List<Double>> distancesBetweenPairChromosomes=new HashMap<String, List<Double>>();
				List<String[]> nextPopulationReduced=nextPopulation;
				
				//Calculate distances between pair of chromosomes
				for(int i=0;i<nextPopulation.size();i++)
				{
					for(int j=0;j<nextPopulation.size();j++)
					{
						distancesBetweenPairChromosomes.put(nextPopulation.get(i)[0]+"-"+nextPopulation.get(j)[0], distancesBetweenPairChromosomes(nextPopulation.get(i),nextPopulation.get(j)));
					}
				}
				
				//Iteratively reduce chromosomes by looking for the less distant chromosome... one by one
				//Calculate number of less/minimal distances for each chromosome
				Map<String, Integer> chromosomesNumberOfLessDistances=new HashMap<String, Integer>();
				Map<String, Integer> chromosomesNumberOfLessDistancesOrdered=new HashMap<String, Integer>();
				List<Double> measureDistances=new ArrayList<Double>();
				boolean lessDistance=false;
				int numberOfLessDistances=0;
				
				for(Map.Entry entry: distancesBetweenPairChromosomes.entrySet())
				{
					String key=(String)entry.getKey();
					List<Double> distances=(List<Double>)entry.getValue();
					chromosomesNumberOfLessDistances.put(key.split("-")[0], 0);
					numberOfLessDistances=0;
					
					for(int i=0;i<distances.size();i++)
					{
						measureDistances.clear();
						
						for(Map.Entry entry2: distancesBetweenPairChromosomes.entrySet())
						{
							String key2=(String)entry2.getKey();
							List<Double> distances2=(List<Double>)entry2.getValue();
							
							if(!key2.equals(key))
							{
								measureDistances.add(distances2.get(i));
							}
						}
						
						lessDistance=false;
						for(int j=0;j<measureDistances.size();j++)
						{
							if(distances.get(i)<=measureDistances.get(j))
							{
								lessDistance=true;
							}
							
							else
							{
								lessDistance=false;
								break;
							}
						}
						
						if(lessDistance)
						{
							numberOfLessDistances++;
							chromosomesNumberOfLessDistances.remove(key.split("-")[0]);
							chromosomesNumberOfLessDistances.put(key.split("-")[0],numberOfLessDistances);
						}
					}
				}
				
				//Order chromosomes by the number of less distances they have, top-down
				chromosomesNumberOfLessDistancesOrdered=orderChromosomesByMinimalDistances(chromosomesNumberOfLessDistances);
					
				//Now, remove only one chromosome with the higher number of less distances
				boolean someRemoved=false;
				for(int i=0;i<nextPopulation.size();i++)
				{
					if(!someRemoved)
					{
						for(Map.Entry entry: chromosomesNumberOfLessDistancesOrdered.entrySet())
						{
							String key=(String)entry.getKey();
							
							if(key.equals(nextPopulation.get(i)[0]))
							{
								nextPopulation.remove(i);
								someRemoved=true;
								break;
							}
						}
					}
					
					else
					{
						break;
					}
				}
			}
		}
		
		//Si no es suficiente con los no-dominados, seleccione algunos dominados de acuerdo a rawFitness
		else if(nextPopulation.size()<archiveSize)
		{
			Map<String, Double> orderedDominated=orderChromosomesByRawFitness(dominatedChromosomesRawFitness);
			
			for(Map.Entry entry: orderedDominated.entrySet())
			{
				if(nextPopulation.size()<archiveSize)
				{
					String key=(String)entry.getKey();
					
					for(int i=0;i<dominatedChromosomes.size();i++)
					{
						if(key.equals(dominatedChromosomes.get(i)[0]))
						{
							nextPopulation.add(dominatedChromosomes.get(i));
							break;
						}
					}
				}
				
				else
				{
					break;
				}
			}

		}
		
		return nextPopulation;
		
	}

	//Calcule la distancia entre un par de cromosomas; sirve para determinar las cromosomas que se van a ir eliminando si el número de no-dominadas es mayor al tamaño del archivo
	public List<Double> distancesBetweenPairChromosomes(String[] chromosome1, String[] chromosome2)
	{
		List<Double> distances=new ArrayList<Double>();
		Double[] chromosomeDifferencesSquare=new Double[chromosome2.length-1];
		Double chromosomeDifferencesSum=0.0;
		Double chromosomeDistance=0.0;
		
		for(int j=1;j<chromosome2.length;j++)
		{
			chromosomeDifferencesSquare[j-1]=(Math.pow(Double.parseDouble(chromosome1[j]) - Double.parseDouble(chromosome2[j]),2));
		}
		
		chromosomeDifferencesSum=0.0;
		for(int j=0;j<chromosomeDifferencesSquare.length;j++)
		{
			chromosomeDifferencesSum=chromosomeDifferencesSum+chromosomeDifferencesSquare[j];
		}
		
		chromosomeDistance=Math.sqrt(chromosomeDifferencesSum);
		distances.add(chromosomeDistance);
		
		return distances; 
	}
	
	//For adding dominated individuals to archive file, when it's size is less than expected
	//Order by raw fitness, where a lesser fitness is better
	  public static Map<String, Double> orderChromosomesByRawFitness(Map<String, Double> chromosomes)
	  {
		  LinkedHashMap<String, Double> sortedChromosomes=new LinkedHashMap<String, Double>();
		  
		  List list=new LinkedList(chromosomes.entrySet());
		  Collections.sort(list, new Comparator()
		  {
			public int compare(Object o1, Object o2)
			{
				return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
			}
		  });
		  
		  for(Iterator it=list.iterator();it.hasNext();)
		  {
			  Map.Entry entry=(Map.Entry)it.next();
			  sortedChromosomes.put((String)entry.getKey(),(Double)entry.getValue());
		  }
		  
		  return sortedChromosomes;
	  }
	  
	//For adding dominated individuals to archive file, when it's size is less than expected
	  public static Map<String, Integer> orderChromosomesByMinimalDistances(Map<String, Integer> chromosomes)
	  {
		  Map<String, Integer> sortedChromosomes=new HashMap<String, Integer>();
		  
		  List list=new LinkedList(chromosomes.entrySet());
		  Collections.sort(list, new Comparator()
		  {
			public int compare(Object o1, Object o2)
			{
				return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
			}
		  });
		  
		  for(Iterator it=list.iterator();it.hasNext();)
		  {
			  Map.Entry entry=(Map.Entry)it.next();
			  sortedChromosomes.put((String)entry.getKey(),(Integer)entry.getValue());
		  }
		  
		  return sortedChromosomes;
	  }

	
	  //For truncating non-dominated individuals from archive file, when it's size is more than expected
	  public List<Double> calculateChromosomeDistancesForTruncating(String[] chromosome, List<String[]> chromosomes)
	  {
		  List<Double> chromosomeDistances=new ArrayList<Double>();
		  Double[] chromosomeDifferencesSquare=new Double[chromosome.length-1];
		  Double chromosomeDifferencesSum=0.0;
		  Double chromosomeDistance=0.0;
			
		  //Extract distances with respect to rest of chromosomes
		  for(int i=0;i<chromosomes.size();i++)
		  {
			  if(!chromosome.equals(chromosomes.get(i)))
			  {
				  for(int j=1;j<chromosome.length;j++)
				  {
					  chromosomeDifferencesSquare[j-1]=(Math.pow(Double.parseDouble(chromosome[j]) - Double.parseDouble(chromosomes.get(i)[j]),2));
				  }
					
				  chromosomeDifferencesSum=0.0;
				  
				  for(int j=0;j<chromosomeDifferencesSquare.length;j++)
				  {
					  chromosomeDifferencesSum=chromosomeDifferencesSum+chromosomeDifferencesSquare[j];
				  }
					
				  chromosomeDistance=Math.sqrt(chromosomeDifferencesSum);
				  chromosomeDistances.add(chromosomeDistance);
			  }
		  }
		return chromosomeDistances;
	  }

	  
	//Perform binary tournament selection (STEP 5), by using the population in archive file
	//Allows selecting the best individuals for making mutation and crossover
	public List<String[]> performBinaryTournamentSelection(List<String[]> population)
	{
		List<String[]> matingPool=new ArrayList<String[]>();
		
		if(population.size()>=2)
		{
			int populationSize=population.size() / 2;
			Random random=new Random();
			int rand1=0;
			int rand2=0;
			int betterChromosome=1;
			String[] chromosome1=population.get(0);
			String[] chromosome2=population.get(1);
			HashSet usedNumbers=new HashSet();
	
			//Llene la población sobre la que se basarán las operaciones de mutación y crossover
			while(matingPool.size()<populationSize  && populationSize!=0)
			{
				usedNumbers.clear();
				
				rand1=random.nextInt(population.size());
				usedNumbers.add(rand1);
				do
				{
					rand2=random.nextInt(population.size());
				}while(!usedNumbers.add(rand2));
						
				chromosome1=population.get(rand1);
				chromosome2=population.get(rand2);
					
				betterChromosome=compareChromosomesFitness(chromosome1,chromosome2);
					
				if(betterChromosome==1)
				{
					matingPool.add(chromosome1);
				}
					
				else 
				{
					matingPool.add(chromosome2);
				}
			}
		}
		return matingPool;
	}
	
	//Comparar fitness entre dos cromosomas específicas; sirve para STEP 2
	public int compareChromosomesFitness(String[] chromosome1, String[] chromosome2)
	{
		int comparison=1;
		boolean dominance=false;
		double chromosome1Fitness=0.0;
		double chromosome2Fitness=0.0;
		
		if(nonDominatedChromosomesRawFitness.containsKey(chromosome1[0]))
		{
			chromosome1Fitness=nonDominatedChromosomesRawFitness.get(chromosome1[0]);;
		}
		
		else
		{
			chromosome1Fitness=dominatedChromosomesRawFitness.get(chromosome1[0]);;
		}

		if(nonDominatedChromosomesRawFitness.containsKey(chromosome2[0]))
		{
			chromosome2Fitness=nonDominatedChromosomesRawFitness.get(chromosome2[0]);;
		}
		
		else
		{
			chromosome2Fitness=dominatedChromosomesRawFitness.get(chromosome2[0]);;
		}
		
		if(chromosome1Fitness<=chromosome2Fitness)
		{
			comparison=1;
		}
			
		else 
		{
			comparison=2;
		}
		
		return comparison;
	}
	
	
	public List<String[]> performMutationAndCrossover(List<String[]> population)
	{
		List<String[]> populationToPerformOperations=new ArrayList<String[]>();
		List<String[]> binaryPopulation=new ArrayList<String[]>();
		List<String[]> newPopulation=new ArrayList<String[]>();
		Map<String, Double> populationRawFitness=new HashMap<String, Double>();
		int i=0;
		
		//Añada todas las cromosomas al raw fitness
		populationRawFitness.putAll(nonDominatedChromosomesFitness);
		populationRawFitness.putAll(dominatedChromosomesFitness);
		
		//Ordene todas las cromosomas por raw fitness 
		populationRawFitness=orderChromosomesByRawFitness(populationRawFitness);
	
		//Escoja los mejores 50% y añadalos a la próxima generación iguales
		for(Map.Entry entry: populationRawFitness.entrySet())
		{
			if(i<(population.size()/2))
			{
				String key=(String)entry.getKey();
					
				for(int j=0;j<population.size();j++)
				{
					if(key.equals(population.get(j)[0]) && !newPopulation.contains(population.get(j)))
					{
						newPopulation.add(population.get(j));
						i++;
						break;
					}
				}
			}
			
			else
			{
				break;
			}
		}
		
		//Ahora, trabaje con el restante 50%
		//Escoja solo los peores 50% para hacer las operaciones (elitism: conserve siempre los mejores)
		for(i=0;i<population.size();i++)
		{
			if(!newPopulation.contains(population.get(i)))
			{
				populationToPerformOperations.add(population.get(i));
			}
		}
		
		//Get binary population
		for(i=0;i<populationToPerformOperations.size();i++)
		{
			binaryPopulation.add(convertChromosomeToBinary(populationToPerformOperations.get(i)));
		}

		//Now, perform operations (mutation and crossover) over the selected population (half mutation, half crossover)
		//First, mutation
		for(i=0;i<(binaryPopulation.size()/2);i++)
		{
			newPopulation.add(performMutation(binaryPopulation.get(i)));
		}
		
		//Second, crossover
		for(i=(binaryPopulation.size()-1);i>=((binaryPopulation.size())/2);i=(i-2))
		{
			//Add the two crossed chromosomes
			if(i!=(binaryPopulation.size()/2))
			{
				Map<String[], String[]> crossedChromosomes=performCrossover(binaryPopulation.get(i),binaryPopulation.get(i-1));
				
				for(Map.Entry entry: crossedChromosomes.entrySet())
				{
					newPopulation.add((String[]) entry.getKey());
					newPopulation.add((String[]) entry.getValue());
				}
			}

			//Add a single chromosome, for maintaning population size
			else 
			{
				Map<String[], String[]> crossedChromosomes=performCrossover(binaryPopulation.get(i),binaryPopulation.get(i));
				
				for(Map.Entry entry: crossedChromosomes.entrySet())
				{
					newPopulation.add((String[]) entry.getKey());
				}
			}
		}

	
		return newPopulation;
	}
	
	//Make mutation - STEP 6 - Only one bit
	public String[] performMutation(String[] chromosome)
	{
		String[] newChromosome=new String[chromosome.length];
		Random random=new Random();
		int randomMeasure=0;
		int numberOfPositionsToFlip=0;
		int initialPositionToFlip=0;
		int finalPositionToFlip=0;

		
		randomMeasure=random.nextInt(chromosome.length);
		
		//If random number equals chromosome name
		if(randomMeasure==0)
		{
			randomMeasure=1;
		}
		
		//Character array for storing flipped values
		char[] flipped=new char[chromosome[randomMeasure].length()];
		
		//If chromosome at random position equals 0
		if(chromosome[randomMeasure].length()!=1)
		{
			numberOfPositionsToFlip=random.nextInt(chromosome[randomMeasure].length());
			initialPositionToFlip=random.nextInt(chromosome[randomMeasure].length()/2);
			finalPositionToFlip=initialPositionToFlip+numberOfPositionsToFlip;
		}
		
		else
		{
			numberOfPositionsToFlip=1;
			initialPositionToFlip=0;
			finalPositionToFlip=0;
		}
		
		//Initialize
		newChromosome=chromosome;
		
		//Now, flip the corresponding measure within the corresponding bits
		for(int i=0;i<chromosome[randomMeasure].length();i++)
		{
			if(i>=initialPositionToFlip && i<=finalPositionToFlip)
			{
				if(chromosome[randomMeasure].charAt(i)=='0')
				{
					flipped[i]='1';
				}
					
				else
				{
					flipped[i]='0';
				}
			}
			
			else
			{
				flipped[i]=chromosome[randomMeasure].charAt(i);
			}
		}
		
		//And assign the flipped measure to new chromosome
		newChromosome[randomMeasure]=String.valueOf(flipped);
		
		//And now convert the chromosome measures back to double
		newChromosome=convertChromosomeToDecimal(newChromosome);
		return newChromosome;
	}
	
	//Make crossover - STEP 6
	public Map<String[], String[]> performCrossover(String[] chromosome1, String[] chromosome2)
	{
		String[] newChromosome1=new String[chromosome1.length];
		String[] newChromosome2=new String[chromosome2.length];
		Map<String[], String[]> resultingChromosomes=new HashMap<String[], String[]>();

		Random random=new Random();
		int randomMeasure=0;
		int numberOfPositionsToFlip=0;
		int initialPositionToFlip=0;
		int finalPositionToFlip=0;
	
		randomMeasure=random.nextInt(chromosome1.length);
		
		//If random position selected equals individual name
		if(randomMeasure==0)
		{
			randomMeasure=1;
		}
		
		//Character array for storing flipped values
		char[] flipped1=new char[chromosome1[randomMeasure].length()];
		char[] flipped2=new char[chromosome2[randomMeasure].length()];
		
		//Select min length to make the bits exchange
		if(chromosome1[randomMeasure].length() <= chromosome2[randomMeasure].length())
		{
			if(chromosome1[randomMeasure].length()!=1)
			{
				numberOfPositionsToFlip=random.nextInt(chromosome1[randomMeasure].length());
				initialPositionToFlip=random.nextInt(chromosome1[randomMeasure].length()/2);
				finalPositionToFlip=initialPositionToFlip+numberOfPositionsToFlip;
			}
			
			else
			{
				numberOfPositionsToFlip=1;
				initialPositionToFlip=0;
				finalPositionToFlip=0;				
			}
		}
		
		else
		{
			if(chromosome2[randomMeasure].length()!=1)
			{
				numberOfPositionsToFlip=random.nextInt(chromosome2[randomMeasure].length());
				initialPositionToFlip=random.nextInt(chromosome2[randomMeasure].length()/2);
				finalPositionToFlip=initialPositionToFlip+numberOfPositionsToFlip;
			}
			
			else
			{
				numberOfPositionsToFlip=1;
				initialPositionToFlip=0;
				finalPositionToFlip=0;				
			}
		}
		
		//Initialize
		newChromosome1=chromosome1;
		newChromosome2=chromosome2;
		
		//Now, flip the corresponding measure within the corresponding bits
		if(chromosome1[randomMeasure].length() <= chromosome2[randomMeasure].length())
		{
			int i=0;
			for(i=0;i<chromosome1[randomMeasure].length();i++)
			{
				if(i>=initialPositionToFlip && i<=finalPositionToFlip)
				{
					flipped1[i]=chromosome2[randomMeasure].charAt(i);
					flipped2[i]=chromosome1[randomMeasure].charAt(i);
				}
				
				else
				{
					flipped1[i]=chromosome1[randomMeasure].charAt(i);
					flipped2[i]=chromosome2[randomMeasure].charAt(i);
				}
			}
			
			for(int j=i;j<chromosome2[randomMeasure].length();j++)
			{
				flipped2[j]=chromosome2[randomMeasure].charAt(j);
			}
		}
		
		else
		{
			int i=0;
			for(i=0;i<chromosome2[randomMeasure].length();i++)
			{
				if(i>=initialPositionToFlip && i<=finalPositionToFlip)
				{
					flipped1[i]=chromosome2[randomMeasure].charAt(i);
					flipped2[i]=chromosome1[randomMeasure].charAt(i);
				}
				
				else
				{
					flipped1[i]=chromosome1[randomMeasure].charAt(i);
					flipped2[i]=chromosome2[randomMeasure].charAt(i);
				}
			}
			
			for(int j=i;j<chromosome1[randomMeasure].length();j++)
			{
				flipped1[j]=chromosome1[randomMeasure].charAt(j);
			}
		}
		
		//Obtain final binary chromosomes
		newChromosome1[randomMeasure]=String.valueOf(flipped1);
		newChromosome2[randomMeasure]=String.valueOf(flipped2);
		
		//Now convert binary chromosomes back to decimal
		newChromosome1=convertChromosomeToDecimal(newChromosome1);
		newChromosome2=convertChromosomeToDecimal(newChromosome2);
		
		resultingChromosomes.put(newChromosome1, newChromosome2);
		return resultingChromosomes;	
	}

	//From decimal to binary
	public String[] convertChromosomeToBinary(String[] chromosome)
	{
		String[] binaryChromosome=new String[chromosome.length];
		binaryChromosome[0]=chromosome[0];
		
		for(int i=1;i<chromosome.length;i++)
		{
			binaryChromosome[i]=(Long.toBinaryString(Double.doubleToRawLongBits(Double.valueOf(chromosome[i]))));
		}
		
		return binaryChromosome;
	}
	
	//From binary to decimal
	public String[] convertChromosomeToDecimal(String[] chromosome)
	{
		String[] decimalChromosome=new String[chromosome.length];
		decimalChromosome[0]=chromosome[0];
		double doubleValue=0.0;
		
		for(int i=1;i<chromosome.length;i++)
		{
			try
			{
				doubleValue=Double.longBitsToDouble((Long.parseLong(chromosome[i],2)));
				doubleValue=Double.valueOf(new DecimalFormat("#.##").format(doubleValue));
				decimalChromosome[i]=String.valueOf(doubleValue);
			}
			
			catch(Exception ex)
			{
				decimalChromosome[i]=String.valueOf(doubleValue);
			}
		}
		
		return decimalChromosome;
	}

	
	//Methods for writting information
	public void writeDominatedChromosomes(String[] chromosome, int isDominated, int currentIteration, boolean first)
	{
		try 
		{
			String outputFile="file-dominatedItemsets-"+currentIteration+".csv";
			CsvWriter writer=null;
			//Append
			if(!first)
			{
				writer=new CsvWriter(new FileWriter(outputFile, true), ',');
			}

			else
			{
				writer=new CsvWriter(new FileWriter(outputFile, false), ',');
			}
	
			for(int j=0;j<chromosome.length;j++)
			{
				writer.write(String.valueOf(chromosome[j]));
			}
			
			writer.write("dominatedBy-"+isDominated);
			writer.endRecord();
			writer.close();
		}
		
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void writeNonDominatedChromosomes(String[] chromosome, int currentIteration, boolean first)
	{
		try 
		{
			String outputFile="file-nonDominatedItemsets-"+currentIteration+".csv";
			
			CsvWriter writer=null;
			//Append
			if(!first)
			{
				writer=new CsvWriter(new FileWriter(outputFile, true), ',');
			}
			
			else
			{
				writer=new CsvWriter(new FileWriter(outputFile, false), ',');
			}
	
			for(int j=0;j<chromosome.length;j++)
			{
				writer.write(String.valueOf(chromosome[j]));
			}
			writer.endRecord();
			writer.close();
		}
		
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	
	public void writeDominatingChromosomes(String[] chromosome, int isDominating, int currentIteration, boolean first)
	{
		try 
		{
			String outputFile="file-dominatingItemsets-"+currentIteration+".csv";

			CsvWriter writer=null;
			//Append
			if(!first)
			{
				writer=new CsvWriter(new FileWriter(outputFile, true), ',');
			}
			
			else 
			{
				writer=new CsvWriter(new FileWriter(outputFile, false), ',');
			}
			
			for(int j=0;j<chromosome.length;j++)
			{
				writer.write(String.valueOf(chromosome[j]));
			}
			writer.write("dominates-"+isDominating);
			writer.endRecord();
			writer.close();
		}
		
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void writeDominatingDominatedChromosomes(String[] dominatingChromosome, String[] dominatedChromosome, boolean first)
	{
		try 
		{
			String outputFile="file-dominatingDominatedItemsets.csv";
			CsvWriter writer=null;
			//Append

			if(!first)
			{
				writer=new CsvWriter(new FileWriter(outputFile, true), ',');
			}
			
			else 
			{
				writer=new CsvWriter(new FileWriter(outputFile, false), ',');
			}
			

			for(int j=0;j<dominatingChromosome.length;j++)
			{
				writer.write(String.valueOf(dominatingChromosome[j]));
			}
			
			for(int j=0;j<dominatingChromosome.length;j++)
			{
				writer.write(String.valueOf(dominatedChromosome[j]));
			}
			writer.endRecord();
			writer.close();
		}
		
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void writeNonDominatedFitness(String nonDominated, double fitness, int currentIteration, boolean first, int currentGeneration)
	{
		try 
		{
			String outputFile="file-nonDominatedItemsetsFitness-"+currentIteration+".csv";
			
			CsvWriter writer=null;
			//Append
			if(!first)
			{
				writer=new CsvWriter(new FileWriter(outputFile, true), ',');
			}
			
			else 
			{
				writer=new CsvWriter(new FileWriter(outputFile, false), ',');
			}

			writer.write(String.valueOf(currentGeneration));
			writer.write(nonDominated);
			writer.write(String.valueOf(fitness));
			writer.endRecord();
			writer.close();
		}
		
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void writeDominatedFitness(String dominated, double fitness, int currentIteration, boolean first, int currentGeneration)
	{
		try 
		{
			String outputFile="file-dominatedItemsetsFitness-"+currentIteration+".csv";
		
			CsvWriter writer=null;
			//Append

			if(!first)
			{
				writer=new CsvWriter(new FileWriter(outputFile, true), ',');
			}
			
			else 
			{
				writer=new CsvWriter(new FileWriter(outputFile, false), ',');
			}			
	
			writer.write(String.valueOf(currentGeneration));
			writer.write(dominated);
			writer.write(String.valueOf(fitness));
			writer.endRecord();
			writer.close();
		}
		
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void writePopulation(List<String[]> population, int currentIteration)
	{
		try 
		{
			String outputFile="currentGeneration.csv";
			CsvWriter writer=new CsvWriter(new FileWriter(outputFile, true), ',');
			
			for(int i=0;i<population.size();i++)
			{
				writer.write(String.valueOf(currentIteration));
				
				for(int j=0;j<population.get(i).length;j++)
				{
					if(j==0)
					{
						writer.write(population.get(i)[j]);
					}
					
					else
					{
						writer.write(String.valueOf(Double.valueOf(population.get(i)[j])));
					}
				}
				writer.endRecord();
			}
			
			writer.close();
		}
		
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	//Final write method; writes best-fitted chromosomes, with their raw fitness; it is used to compare current itemsets with randomly adjusted itemsets by SPEA2
	public void writeParetoFrontier(String generationsFile, int currentIteration)
	{
		try 
		{
			String outputFile="file-OrderedItemSets-Iteration"+currentIteration+".csv";
			CsvWriter writer=new CsvWriter(new FileWriter(outputFile, false), ',');
			Map<String, Double> chromosomesFitness=new HashMap<String, Double>();
			int position=1;
			
			//Get chromosomes raw fitness (the best one)
			CsvReader readerFitnessNonDominated=new CsvReader("file-nonDominatedItemsetsFitness-"+currentIteration+".csv");
			CsvReader readerFitnessDominated=new CsvReader("file-dominatedItemsetsFitness-"+currentIteration+".csv");
			
			//First get dominated raw fitness
			while(readerFitnessDominated.readRecord())
			{
				if(!chromosomesFitness.containsKey(readerFitnessDominated.get(1)))
				{
					chromosomesFitness.put(readerFitnessDominated.get(1), Double.valueOf(readerFitnessDominated.get(2)));
				}
				
				else
				{
					chromosomesFitness.remove(readerFitnessDominated.get(1));
					chromosomesFitness.put(readerFitnessDominated.get(1), Double.valueOf(readerFitnessDominated.get(2)));
				}
			}
			
			//Now get non-dominated raw fitness
			while(readerFitnessNonDominated.readRecord())
			{
				if(!chromosomesFitness.containsKey(readerFitnessNonDominated.get(1)))
				{
					chromosomesFitness.put(readerFitnessNonDominated.get(1), Double.valueOf(readerFitnessNonDominated.get(2)));
				}
				
				else
				{
					chromosomesFitness.remove(readerFitnessNonDominated.get(1));
					chromosomesFitness.put(readerFitnessNonDominated.get(1), Double.valueOf(readerFitnessNonDominated.get(2)));
				}
			}
			
			readerFitnessNonDominated.close();
			readerFitnessDominated.close();

			//Now, order by fitness
			chromosomesFitness=orderChromosomesByRawFitness(chromosomesFitness);

			for(Map.Entry entry: chromosomesFitness.entrySet())
			{
				String key=(String)entry.getKey();
				Double fitness=(Double)entry.getValue();
				
				//Now, iterate through generations files in order to get chromosome and measures values
				CsvReader reader=new CsvReader(generationsFile);
				while(reader.readRecord())
				{
					//Get chromosome in file
					String[] values=reader.getValues();
					
					if(key.equals(values[1]))
					{
						writer.write(String.valueOf(fitness));
						//Get chromosome name and measures values
						for(int i=1;i<values.length;i++)
						{
							writer.write(values[i]);
						}
						writer.write(String.valueOf(position));
						reader.close();
						
						//Here, determine whether the solution is potential to eliminate!
						
						
						writer.endRecord();
						
						position++;
						break;
					}
				}
			}
			writer.close();
		}
		
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
