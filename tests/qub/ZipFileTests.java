package qub;

public interface ZipFileTests
{
    public static void test(TestRunner runner)
    {
        runner.testGroup(ZipFile.class, () ->
        {
            runner.testGroup("get(File)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> ZipFile.get(null),
                        new PreConditionFailure("file cannot be null."));
                });

                runner.test("with non-null",
                    (TestResources resources) -> Tuple.create(resources.createFakeDesktopProcess()),
                    (Test test, FakeDesktopProcess process) ->
                {
                    final Folder currentFolder = process.getCurrentFolder();
                    final File file = currentFolder.getFile("test.zip").await();

                    final ZipFile zipFile = ZipFile.get(file);

                    test.assertNotNull(zipFile);
                    test.assertEqual(file, zipFile);
                });
            });

            runner.testGroup("iterateEntries()", () ->
            {
                runner.test("with file that doesn't exist",
                    (TestResources resources) -> Tuple.create(resources.createFakeDesktopProcess()),
                    (Test test, FakeDesktopProcess process) ->
                {
                    final Folder folder = process.getCurrentFolder();
                    final ZipFile file = ZipFile.get(folder.getFile("test.zip").await());
                    
                    final ZipEntryIterator iterator = file.iterateEntries();
                    IteratorTests.assertIterator(test, iterator, false, null);
                    test.assertFalse(iterator.isDisposed());

                    test.assertThrows(() -> iterator.next(),
                        new FileNotFoundException(file));

                    IteratorTests.assertIterator(test, iterator, false, null);
                    test.assertFalse(iterator.isDisposed());
                });

                runner.test("with empty file",
                    (TestResources resources) -> Tuple.create(resources.createFakeDesktopProcess()),
                    (Test test, FakeDesktopProcess process) ->
                {
                    final Folder folder = process.getCurrentFolder();
                    final ZipFile file = ZipFile.get(folder.getFile("test.zip").await());
                    file.create().await();
                    
                    final ZipEntryIterator iterator = file.iterateEntries();
                    IteratorTests.assertIterator(test, iterator, false, null);
                    test.assertFalse(iterator.isDisposed());

                    test.assertFalse(iterator.next());
                    
                    IteratorTests.assertIterator(test, iterator, true, null);
                    test.assertTrue(iterator.isDisposed());
                });

                runner.test("with non-empty non-zip file",
                    (TestResources resources) -> Tuple.create(resources.createFakeDesktopProcess()),
                    (Test test, FakeDesktopProcess process) ->
                {
                    final Folder folder = process.getCurrentFolder();
                    final ZipFile file = ZipFile.get(folder.getFile("test.zip").await());
                    file.setContentsAsString("I'm not a zip file!").await();
                    
                    final ZipEntryIterator iterator = file.iterateEntries();
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
                    try (final ZipEntryIterator iterator = zipFile.iterateEntries())
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

            runner.testGroup("getZipContentsWriteStream()", () ->
            {
                runner.test("with file that doesn't exist",
                    (TestResources resources) -> Tuple.create(resources.createFakeDesktopProcess()),
                    (Test test, FakeDesktopProcess process) ->
                {
                    final Folder folder = process.getCurrentFolder();
                    final ZipFile file = ZipFile.get(folder.getFile("test.zip").await());
                    
                    try (final ZipWriteStream writeStream = file.getContentsZipWriteStream().await())
                    {
                        test.assertTrue(file.exists().await());

                        try (final ZipEntryWriteStream entryWriteStream = writeStream.createEntryWriteStream("hello/world"))
                        {
                            entryWriteStream.write("Hello world!").await();
                        }

                        try (final ZipEntryWriteStream entryWriteStream = writeStream.createEntryWriteStream("hello/there"))
                        {
                            entryWriteStream.write("Hello there!").await();
                        }

                        writeStream
                            .createEntry("my/folder/", (ZipEntryWriteStream entryWriteStream) -> {})
                            .createEntry(ZipEntryParameters.create()
                                .setEntryPath("my/folder/stuff")
                                .setComment("My special comment")
                                .setLastModified(DateTime.create(1950, 1, 2)),
                                (ZipEntryWriteStream entryWriteStream) ->
                                {
                                    entryWriteStream.writeLine("Look! My stuff!").await();
                                });
                    }
                    test.assertTrue(file.exists().await());

                    try (final ZipEntryIterator entries = file.iterateEntries())
                    {
                        test.assertTrue(entries.next());
                        try (final ZipEntryReadStream entryReadStream1 = entries.getCurrent())
                        {
                            test.assertNotNull(entryReadStream1);
                            test.assertFalse(entryReadStream1.isDisposed());
                            test.assertEqual(Path.parse("hello/world"), entryReadStream1.getPath());
                            test.assertFalse(entryReadStream1.isDirectory());
                            test.assertNull(entryReadStream1.getComment());
                            test.assertEqual(CharacterEncoding.UTF_8, entryReadStream1.getCharacterEncoding());
                            test.assertEqual(DataSize.bytes(-1), entryReadStream1.getCompressedSize());
                            test.assertEqual(DataSize.bytes(-1), entryReadStream1.getUncompressedSize());
                            test.assertNotNull(entryReadStream1.getLastModified());
                            test.assertEqual("Hello world!", entryReadStream1.readEntireString().await());
                        }

                        test.assertTrue(entries.next());
                        try (final ZipEntryReadStream entryReadStream2 = entries.getCurrent())
                        {
                            test.assertNotNull(entryReadStream2);
                            test.assertFalse(entryReadStream2.isDisposed());
                            test.assertEqual(Path.parse("hello/there"), entryReadStream2.getPath());
                            test.assertFalse(entryReadStream2.isDirectory());
                            test.assertNull(entryReadStream2.getComment());
                            test.assertEqual(CharacterEncoding.UTF_8, entryReadStream2.getCharacterEncoding());
                            test.assertEqual(DataSize.bytes(-1), entryReadStream2.getCompressedSize());
                            test.assertEqual(DataSize.bytes(-1), entryReadStream2.getUncompressedSize());
                            test.assertNotNull(entryReadStream2.getLastModified());
                            test.assertEqual("Hello there!", entryReadStream2.readEntireString().await());
                        }

                        test.assertTrue(entries.next());
                        try (final ZipEntryReadStream entryReadStream3 = entries.getCurrent())
                        {
                            test.assertNotNull(entryReadStream3);
                            test.assertFalse(entryReadStream3.isDisposed());
                            test.assertEqual(Path.parse("my/folder/"), entryReadStream3.getPath());
                            test.assertTrue(entryReadStream3.isDirectory());
                            test.assertNull(entryReadStream3.getComment());
                            test.assertEqual(CharacterEncoding.UTF_8, entryReadStream3.getCharacterEncoding());
                            test.assertEqual(DataSize.bytes(-1), entryReadStream3.getCompressedSize());
                            test.assertEqual(DataSize.bytes(-1), entryReadStream3.getUncompressedSize());
                            test.assertNotNull(entryReadStream3.getLastModified());
                            test.assertEqual("", entryReadStream3.readEntireString().await());
                        }

                        test.assertTrue(entries.next());
                        try (final ZipEntryReadStream entryReadStream4 = entries.getCurrent())
                        {
                            test.assertNotNull(entryReadStream4);
                            test.assertFalse(entryReadStream4.isDisposed());
                            test.assertEqual(Path.parse("my/folder/stuff"), entryReadStream4.getPath());
                            test.assertFalse(entryReadStream4.isDirectory());
                            test.assertNull(entryReadStream4.getComment());
                            test.assertEqual(CharacterEncoding.UTF_8, entryReadStream4.getCharacterEncoding());
                            test.assertEqual(DataSize.bytes(-1), entryReadStream4.getCompressedSize());
                            test.assertEqual(DataSize.bytes(-1), entryReadStream4.getUncompressedSize());
                            test.assertEqual(DateTime.create(1950, 1, 2), entryReadStream4.getLastModified());
                            test.assertEqual("Look! My stuff!\n", entryReadStream4.readEntireString().await());
                        }

                        test.assertFalse(entries.next());
                    }
                });
            });
        });
    }
}
