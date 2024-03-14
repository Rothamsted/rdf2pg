package uk.ac.rothamsted.neo4j.utils;

import java.util.concurrent.CompletionStage;

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.BaseSession;
import org.neo4j.driver.BookmarkManager;
import org.neo4j.driver.Driver;
import org.neo4j.driver.ExecutableQuery;
import org.neo4j.driver.Metrics;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.reactive.ReactiveSession;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.types.TypeSystem;

/**
 * 
 * The Neo4j Driver extension
 * 
 * An extended version of the {@link Driver Neo4j driver class}, which has 
 * some utilities, such as keeping a default DB name and using it with a 
 * {@link #defaultSessionConfig()}.
 *
 * @author Marco Brandizi
 * <dl><dt>Date:</dt><dd>13 Mar 2024</dd></dl>
 *
 */
public class XNeo4jDriver implements Driver
{
	private Driver driver;
	private SessionConfig defaultSessionConfig;
		
	public XNeo4jDriver ( Driver driver, String databaseName )
	{
		super ();
		this.driver = driver;
		this.defaultSessionConfig = databaseName == null 
			? SessionConfig.defaultConfig ()
			: SessionConfig.forDatabase ( databaseName );
	}
	
	/**
	 * When database name is null, defaults to the usual default session config.
	 * 
	 * @see #defaultSessionConfig()
	 * 
	 */
	public XNeo4jDriver ( Driver driver ) {
		this ( driver, null );
	}

	/**
	 * A default config that sets the database name to the one passed via constructor.
	 * 
	 * All session-creation methods that don't have a config parameter are invoked
	 * with this default, eg, {@link #session()}, {@link #session(Class)} create
	 * sessions that use this.
	 * 
	 * The original database name can be fetched via {@link SessionConfig#database()}.
	 * 
	 * If the database name is null, this defaults to {@link SessionConfig#defaultConfig()}.
	 */
	public SessionConfig defaultSessionConfig ()
	{
		return defaultSessionConfig;
	}
	
	public ExecutableQuery executableQuery ( String query )
	{
		return driver.executableQuery ( query );
	}

	public BookmarkManager executableQueryBookmarkManager ()
	{
		return driver.executableQueryBookmarkManager ();
	}

	public boolean isEncrypted ()
	{
		return driver.isEncrypted ();
	}

	public Session session ()
	{
		return driver.session ( defaultSessionConfig () );
	}

	public Session session ( SessionConfig sessionConfig )
	{
		return driver.session ( sessionConfig );
	}

	public <T extends BaseSession> T session ( Class<T> sessionClass )
	{
		return driver.session ( sessionClass, defaultSessionConfig () );
	}

	public <T extends BaseSession> T session ( Class<T> sessionClass, AuthToken sessionAuthToken )
	{
		return driver.session ( sessionClass, defaultSessionConfig (), sessionAuthToken );
	}

	public <T extends BaseSession> T session ( Class<T> sessionClass, SessionConfig sessionConfig )
	{
		return driver.session ( sessionClass, sessionConfig );
	}

	public <T extends BaseSession> T session ( 
		Class<T> sessionClass, SessionConfig sessionConfig, AuthToken sessionAuthToken )
	{
		return driver.session ( sessionClass, sessionConfig, sessionAuthToken );
	}

	@Deprecated
	public RxSession rxSession ()
	{
		return driver.rxSession ( defaultSessionConfig () );
	}

	@Deprecated
	public RxSession rxSession ( SessionConfig sessionConfig )
	{
		return driver.rxSession ( sessionConfig );
	}

	@Deprecated
	public ReactiveSession reactiveSession ()
	{
		return driver.reactiveSession ( defaultSessionConfig () );
	}

	@Deprecated
	public ReactiveSession reactiveSession ( SessionConfig sessionConfig )
	{
		return driver.reactiveSession ( sessionConfig );
	}

	@Deprecated
	public AsyncSession asyncSession ()
	{
		return driver.asyncSession ( defaultSessionConfig () );
	}

	@Deprecated
	public AsyncSession asyncSession ( SessionConfig sessionConfig )
	{
		return driver.asyncSession ( sessionConfig );
	}

	public void close ()
	{
		driver.close ();
	}

	public CompletionStage<Void> closeAsync ()
	{
		return driver.closeAsync ();
	}

	public Metrics metrics ()
	{
		return driver.metrics ();
	}

	public boolean isMetricsEnabled ()
	{
		return driver.isMetricsEnabled ();
	}

	@Deprecated
	public TypeSystem defaultTypeSystem ()
	{
		return driver.defaultTypeSystem ();
	}

	public void verifyConnectivity ()
	{
		driver.verifyConnectivity ();
	}

	public CompletionStage<Void> verifyConnectivityAsync ()
	{
		return driver.verifyConnectivityAsync ();
	}

	public boolean verifyAuthentication ( AuthToken authToken )
	{
		return driver.verifyAuthentication ( authToken );
	}

	public boolean supportsSessionAuth ()
	{
		return driver.supportsSessionAuth ();
	}

	public boolean supportsMultiDb ()
	{
		return driver.supportsMultiDb ();
	}

	public CompletionStage<Boolean> supportsMultiDbAsync ()
	{
		return driver.supportsMultiDbAsync ();
	}

}
