DISCORD4J := $(wildcard Discord4j-*.jar)

#will need to create file called api-key.txt which contains the api key
APIKEY := $(shell cat api-key.txt) 

JSONDIR = JSON-java

all:	Bot.java clean json
	javac -cp $(DISCORD4J) $(JSONDIR)/*.java Bot.java -Xlint:deprecation

run: 	all
	java -cp ".:$(DISCORD4J)" Bot "$(APIKEY)" "commands.$(JSONDIR)"

json:
	javac -d $(JSONDIR) -cp $(JSONDIR)/ $(JSONDIR)/*.java

clean:
	rm -f *.class json/*.class
