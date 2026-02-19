package com.mongodb.course.m03.bdd;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoConnectionSteps {

    @Autowired
    private MongoTemplate mongoTemplate;

    private Document pingResult;
    private Document insertedDocument;

    @Given("a MongoDB container is running")
    public void aMongoDbContainerIsRunning() {
        assertThat(mongoTemplate).isNotNull();
    }

    @When("I execute a ping command")
    public void iExecuteAPingCommand() {
        pingResult = mongoTemplate.getDb().runCommand(new Document("ping", 1));
    }

    @Then("the response status is ok")
    public void theResponseStatusIsOk() {
        assertThat(pingResult.getDouble("ok")).isEqualTo(1.0);
    }

    @When("I insert a document with name {string} into collection {string}")
    public void iInsertADocumentWithNameIntoCollection(String name, String collection) {
        insertedDocument = new Document("name", name).append("source", "bdd-test");
        mongoTemplate.insert(insertedDocument, collection);
    }

    @Then("I can read the document with name {string} from collection {string}")
    public void iCanReadTheDocumentWithNameFromCollection(String name, String collection) {
        Query query = new Query(Criteria.where("name").is(name));
        Document found = mongoTemplate.findOne(query, Document.class, collection);
        assertThat(found).isNotNull();
        assertThat(found.getString("name")).isEqualTo(name);
    }
}
