SNPParser{
	var <placement;
	var <comboDict;
	var <file;
	var <fileLength;
	var <userID;
  var <linesToRead;
  var <testSet;

	*new{
		arg aFile, aTestSet = inf;
		^super.new.init(aFile, aTestSet);
	}

	init{
		arg aFile, aTestSet;
		this.setFileAndUser(aFile);
    linesToRead = 0;
    testSet = false;
	}

	setFileAndUser{//set the file (again) to be parsed
		arg aFile;
		var allSlash, allDot;
		file = aFile;
		allSlash = file.findAll("/");
		allDot = file.findAll(".");
		if(allSlash.notNil,{//if there is no slash in the filename
			userID = file[allSlash[allSlash.size-1]+1..allDot[allDot.size-2]-1].asInt;
		},{
			userID = file[0..allDot[allDot.size-2]-1].asInt;
		});
		userID.postln;
	}

	readFile{//parse the file this parser currently is setup with
    arg amount = 0;
		var found = false, company = "";
    linesToRead = amount;
    if(linesToRead>0,{
      testSet = true;
    },{
      testSet = false;
    });
  "Guessing parser.".postln;
  SNPInfo.sCompany.do({
			arg cCompany, i;
			if(file.contains(cCompany.asString),{
				if(SNPInfo.workingSCompany.includes(i),{
					("Attempting to parse a "++cCompany++" file now.").postln;
					company = cCompany;
					found = true;
				},{
					("Parsing files from "++cCompany++" is not supported (yet). Get me a coffee to make it work!").postln;
				});
			});
		});
		if(found.not,{
			("This file might not be supported at all: "++file).postln;
		},{
			switch(company,
				"23andme",{^this.parse23andme},
				{"No no no no no!".postln}
			);
		});
	}

	parse23andme{//parsing a 23andme file: id, chromosome, position, base
		var snpFile = File(file, "r"), line = "", counter = 0, tmp, snp, resolver, newSameCounter = 0.0;
		fileLength = (("wc -l "++file.shellQuote).unixCmdGetStdOut).delimit({|ch| ch.isSpace});
		("File "++file++" is "++fileLength[0]++" lines long.").postln;
		"Parsing might require some minutes! So kick back an get a coffee, while it lasts.".postln;
		comboDict = SNPDict.new(fileLength[0], userID);
		if(snpFile.isOpen,{
			protect{
				while{nand((line = snpFile.getLine).notNil, counter>linesToRead)}{//FIXME: Reinsert testset
					if(line[0].asString!="#",{//skip commented lines
						tmp = line.delimit({|ch| ch.isSpace});//delimit the line by space and/or tab
						if(tmp[3].asString!="--" && (SNPInfo.isBasePair(tmp[3]) || SNPInfo.isBase(tmp[3]) && (SNPInfo.chromosomesLength[SNPInfo.convertChromosome(tmp[1])-1]>=tmp[2].asFloat)),{//skip empty SNPs and make sure it's either a single base or a base pair and ignore out-of-range SNPs (yes, science is unclear!)
							if(SNPInfo.isBasePair(tmp[3]),{//if it's a base pair, set it up
                snp = [tmp[1], tmp[2], tmp[0], SNPInfo.baseToVec(tmp[3].asSymbol)]; //creates a SNP information set (chromosome, rsid, base)
                resolver = SNPInfo.createResolverForPair(tmp[3]);
                if (resolver!=[SNPInfo.e,SNPInfo.e],{
                  snp = snp++resolver;
                });
							},{
								if(SNPInfo.isBase(tmp[3]), {//if it's a single base, set it up
                  snp = [tmp[1], tmp[2], tmp[0], tmp[3]]; //creates a SNP information set (chromosome, rsid, base)
								});
							});
							newSameCounter = newSameCounter + comboDict.storeSNP(snp, SNPInfo.calcPosition(snp[0], snp[1]));
              newSameCounter.postln;
              snp.postln;
              resolver.postln;
							switch(newSameCounter,
								1.0,{"Storing SNPs now: \n==========".postln;},
								100000.0,{"=".post;},
								200000.0,{"=".post;},
								300000.0,{"=".post;},
								400000.0,{"=".post;},
								500000.0,{"=".post;},
								600000.0,{"=".post;},
								700000.0,{"=".post;},
								800000.0,{"=".post;},
								900000.0,{"=".post;},
							);
						});
					});
				counter = counter + 1;
				};
			}{
				snpFile.close;
			};
		},{
			("Couldn't open file for reading: "++file).warn;
		});
		"=".postln;
		"Sorting lookup tables. This will also take some time!".postln;
		comboDict.orderLookup(2);
		"Done sorting lookup tables.".postln;
		^comboDict;
	}
}
