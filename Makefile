.PHONY: json

DISCORD4J := $(wildcard Discord4j-*.jar)

#will need to create file called api-key.txt which contains the api key
APIKEY := $(shell cat api-key.txt)

JSONDIR = json

ifeq ("$(wildcard $(JSONDIR))","")
git clone "https://github.com/stleary/JSON-java" $(JSONDIR)
endif

all:	clean json
	javac -cp "$(DISCORD4J):$(JSONDIR)/org/json/" Bot.java -Xlint:deprecation

run: 	all
	java -cp ".:$(DISCORD4J):$(JSONDIR)/org/json/" Bot "$(APIKEY)" "commands.json"

json:
	javac -d $(JSONDIR) -cp $(JSONDIR)/ $(JSONDIR)/*.java

clean:
	rm -f *.class
	rm -rf $(JSONDIR)/org/
