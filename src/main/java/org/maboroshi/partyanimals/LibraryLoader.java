package org.maboroshi.partyanimals;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

public class LibraryLoader implements PluginLoader {
    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addDependency(new Dependency(new DefaultArtifact("com.zaxxer:HikariCP:7.0.2"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.mysql:mysql-connector-j:9.5.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.mariadb.jdbc:mariadb-java-client:3.5.7"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("de.exlll:configlib-paper:4.8.0"), null));

        resolver.addRepository(
                new RemoteRepository.Builder("paper", "default", "https://repo.papermc.io/repository/maven-public/")
                        .build());

        classpathBuilder.addLibrary(resolver);
    }
}
