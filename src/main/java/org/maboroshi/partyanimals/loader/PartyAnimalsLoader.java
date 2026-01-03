package org.maboroshi.partyanimals.loader;

import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;

public class PartyAnimalsLoader implements PluginLoader {
    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addDependency(new Dependency(new DefaultArtifact("org.xerial:sqlite-jdbc:3.51.1.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("de.exlll:configlib-paper:4.8.0"), null));

        resolver.addRepository(new RemoteRepository.Builder(
            "paper",
            "default",
            "https://repo.papermc.io/repository/maven-public/"
        ).build());

        classpathBuilder.addLibrary(resolver);
    }
}
