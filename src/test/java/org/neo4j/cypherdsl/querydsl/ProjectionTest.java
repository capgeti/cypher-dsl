/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypherdsl.querydsl;

import java.io.IOException;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.cypherdsl.grammar.Execute;
import org.neo4j.cypherdsl.querydsl.Projection;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.test.GraphDescription;
import org.neo4j.test.GraphHolder;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.neo4j.test.TestData;

import static org.neo4j.cypherdsl.CypherQuery.*;
import static org.neo4j.cypherdsl.query.neo4j.StartExpressionNeo.*;

/**
 * Set up a query using the CypherQuery builder, and then use it to execute a query to a test database and project the results.
 */
public class ProjectionTest
    implements GraphHolder
{
    public @Rule
    TestData<Map<String, Node>> data = TestData.producedThrough( GraphDescription.createGraphFor(
        this, true ) );

    private ImpermanentGraphDatabase graphdb;
    private ExecutionEngine engine;

    @Test
    @GraphDescription.Graph( value = {
        "John friend Sara", "John friend Joe",
        "Sara friend Maria", "Joe friend Steve"
    }, autoIndexNodes = true )
    public void testCypherExecution()
        throws Exception
    {
        data.get();

        String query = start(lookup("john", "node_auto_index", "name", "John"))
                                              .match(node("john").out("friend").node().out( "friend" ).node( "fof" ))
                                              .returns( identifier( "john" ).property( "name" ), identifier( "fof" ).property( "name" ), identifier( "john" ) )
                                              .toString();

        System.out.println(query);
        ExecutionResult result = engine.execute( query );
        Node john = null;
        for( Map<String, Object> stringObjectMap : result )
        {
            john = ((Node)stringObjectMap.get( "john" ));
        }
        System.out.println( result.toString() );

        {
            Execute q = start( nodeById( "john", john ) )
                                                  .match( node( "john" ).out( "friend" )
                                                              .node()
                                                              .out( "friend" )
                                                              .node( "fof" ) )
                                                  .returns( identifier( "john" ).property( "name" ), identifier( "fof" )
                                                      .property( "name" ) );

            System.out.println(q);
            System.out.println( engine.execute( q.toString() ).toString() );
        }
        
        {
            Projection<Friend> projection = new Projection<Friend>(Friend.class);
            Iterable<Friend> friends = projection.iterable( engine.execute( start( nodeById( "john", john ) )
                                                                                .match( node( "john" ).out( "friend" )
                                                                                            .node()
                                                                                            .out( "friend" )
                                                                                            .node( "fof" ) )
                                                                                .returns( as( identifier( "john" ).property( "name" ) ,"name" ),
                                                                                          as( identifier( "fof" ).property( "name" ) , "friend" ) ).toString() ));
            System.out.println( friends );
        }
    }

    @Before
    public void setup()
        throws IOException
    {
        graphdb = new ImpermanentGraphDatabase();
        graphdb.cleanContent(true);

        engine = new ExecutionEngine( graphdb );
    }

    @Override
    public GraphDatabaseService graphdb()
    {
        return graphdb;
    }

    public static class Friend
    {
        public String name;
        public String friend;

        @Override
        public String toString()
        {
            return name+" is friend with "+friend;
        }
    }
}
