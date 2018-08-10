package com.github.jochenw.icm.core.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class Files {
    public static void deleteDirectory(Path pDir) throws IOException {
    	  java.nio.file.Files.walkFileTree(pDir, new SimpleFileVisitor<Path>() {
    		  @Override
    		  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
    			  java.nio.file.Files.delete(file);
    			  return FileVisitResult.CONTINUE;
    		  }

    		  @Override
    		  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    			  java.nio.file.Files.delete(dir);
    			  return FileVisitResult.CONTINUE;
    		  }
    	  });
    }
}
