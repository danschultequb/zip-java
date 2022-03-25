package qub;

public interface ZipEntryIteratorTests
{
    public static void test(TestRunner runner)
    {
        runner.testGroup(ZipEntryIterator.class, () ->
        {
            runner.testGroup("create(File)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> ZipEntryIterator.create((File)null),
                        new PreConditionFailure("file cannot be null."));
                });

                runner.test("with file that doesn't exist",
                    (TestResources resources) -> Tuple.create(resources.getTemporaryFolder()),
                    (Test test, Folder temporaryFolder) ->
                {
                    final File file = temporaryFolder.getFile("test.zip").await();
                    
                    final ZipEntryIterator iterator = ZipEntryIterator.create(file);
                    IteratorTests.assertIterator(test, iterator, false, null);
                    test.assertFalse(iterator.isDisposed());

                    test.assertThrows(() -> iterator.next(),
                        new FileNotFoundException(file));

                    IteratorTests.assertIterator(test, iterator, false, null);
                    test.assertFalse(iterator.isDisposed());
                });

                runner.test("with empty file",
                    (TestResources resources) -> Tuple.create(resources.getTemporaryFolder()),
                    (Test test, Folder temporaryFolder) ->
                {
                    final File file = temporaryFolder.getFile("test.zip").await();
                    file.create().await();
                    
                    final ZipEntryIterator iterator = ZipEntryIterator.create(file);
                    IteratorTests.assertIterator(test, iterator, false, null);
                    test.assertFalse(iterator.isDisposed());

                    test.assertFalse(iterator.next());
                    
                    IteratorTests.assertIterator(test, iterator, true, null);
                    test.assertTrue(iterator.isDisposed());
                });

                runner.test("with non-empty non-zip file",
                    (TestResources resources) -> Tuple.create(resources.getTemporaryFolder()),
                    (Test test, Folder temporaryFolder) ->
                {
                    final ZipFile file = ZipFile.get(temporaryFolder.getFile("test.zip").await());
                    file.setContentsAsString("I'm not a zip file!").await();
                    
                    final ZipEntryIterator iterator = ZipEntryIterator.create(file);
                    IteratorTests.assertIterator(test, iterator, false, null);
                    test.assertFalse(iterator.isDisposed());

                    test.assertFalse(iterator.next());

                    IteratorTests.assertIterator(test, iterator, true, null);
                    test.assertTrue(iterator.isDisposed());
                });

                runner.test("with actual zip file",
                    (TestResources resources) -> Tuple.create(resources.getFileSystem()),
                    (Test test, FileSystem fileSystem) ->
                {
                    final ZipFile zipFile = ZipFile.get(fileSystem.getFile("C:/qub/qub/lib-java/versions/178/lib-java-sources.zip").await());
                    try (final ZipEntryIterator iterator = ZipEntryIterator.create(zipFile))
                    {
                        IteratorTests.assertIterator(test, iterator, false, null);
                        test.assertFalse(iterator.isDisposed());

                        for (final ZipEntryReadStream entry : iterator)
                        {
                            test.assertNotNull(entry);
                            IteratorTests.assertIterator(test, iterator, true, entry);
                        }

                        IteratorTests.assertIterator(test, iterator, true, null);
                        test.assertTrue(iterator.isDisposed());
                    }
                });
            });

            runner.testGroup("create(ByteReadStream)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> ZipEntryIterator.create((ByteReadStream)null),
                        new PreConditionFailure("readStream cannot be null."));
                });

                runner.test("with empty stream",
                    (TestResources resources) -> Tuple.create(resources.getTemporaryFolder()),
                    (Test test, Folder temporaryFolder) ->
                {
                    final ByteReadStream readStream = InMemoryByteStream.create().endOfStream();
                    
                    final ZipEntryIterator iterator = ZipEntryIterator.create(readStream);
                    IteratorTests.assertIterator(test, iterator, false, null);
                    test.assertFalse(iterator.isDisposed());

                    test.assertFalse(iterator.next());
                    
                    IteratorTests.assertIterator(test, iterator, true, null);
                    test.assertTrue(iterator.isDisposed());
                });

                runner.test("with non-empty non-zip stream",
                    (TestResources resources) -> Tuple.create(resources.getTemporaryFolder()),
                    (Test test, Folder temporaryFolder) ->
                {
                    final ByteReadStream readStream = InMemoryCharacterToByteStream.create("I'm not a zip stream!").endOfStream();
                    
                    final ZipEntryIterator iterator = ZipEntryIterator.create(readStream);
                    IteratorTests.assertIterator(test, iterator, false, null);
                    test.assertFalse(iterator.isDisposed());

                    test.assertFalse(iterator.next());

                    IteratorTests.assertIterator(test, iterator, true, null);
                    test.assertTrue(iterator.isDisposed());
                });

                runner.test("with actual zip stream",
                    (TestResources resources) -> Tuple.create(resources.getFileSystem(), resources.getProcess()),
                    (Test test, FileSystem fileSystem, Process process) ->
                {
                    final ZipFile zipFile = ZipFile.get(fileSystem.getFile("C:/qub/qub/lib-java/versions/178/lib-java-sources.zip").await());
                    try (final ByteReadStream readStream = zipFile.getContentsReadStream().await())
                    {
                        try (final ZipEntryIterator iterator = ZipEntryIterator.create(readStream))
                        {
                            IteratorTests.assertIterator(test, iterator, false, null);
                            test.assertFalse(iterator.isDisposed());

                            for (final ZipEntryReadStream entry : iterator)
                            {
                                test.assertNotNull(entry);
                                IteratorTests.assertIterator(test, iterator, true, entry);

                                test.assertNotNull(entry.getPath());
                                if (entry.isDirectory())
                                {
                                    test.assertTrue(entry.getPath().endsWith("/"));
                                    test.assertEqual(DataSize.zero, entry.getCompressedSize());
                                    test.assertEqual(DataSize.zero, entry.getUncompressedSize());
                                    test.assertThrows(() -> entry.readByte().await(),
                                        new EndOfStreamException());
                                }
                                else
                                {
                                    test.assertFalse(entry.getPath().endsWith("/"));
                                    test.assertGreaterThanOrEqualTo(entry.getCompressedSize(), DataSize.zero);
                                    test.assertGreaterThanOrEqualTo(entry.getUncompressedSize(), DataSize.zero);

                                    if (entry.getCompressedSize().getValue() == 0)
                                    {
                                        test.assertThrows(() -> entry.readByte().await(),
                                            new EndOfStreamException());
                                    }
                                    else
                                    {
                                        final Byte b = entry.readByte().await();
                                        test.assertNotNull(b);
                                    }
                                }

                                test.assertFalse(entry.isDisposed());
                                test.assertNull(entry.getComment());
                                test.assertNotNull(entry.getLastModified());
                            }

                            IteratorTests.assertIterator(test, iterator, true, null);
                            test.assertTrue(iterator.isDisposed());
                        }
                    }
                });
            });
        });
    }
}
