# jnmofsexp1

A simple in-memory FileSystem implementation in Java that implements a subset
of [NIO.2][jdoc-nio-file] interfaces. Only some operations are supported for
now (WIP).

Supported features:

* most operations implemented only using interfaces defined in `java.nio`
  package; this means that [Path][jdoc-path], [FileSystemProvider][jdoc-fsp],
  and [FileSystem][jdoc-fs] could be re-used almost as-is when implementing
  other providers; there are limitations because of gaps in the NIO.2 spec
  (e.g. `FileSystem.getFileStore(Path)`)
* multiple namespaces/roots: this can be thought of as multiple disks/storage
  pools, or multiple subvolumes in a single filesystem
* directories and files observing the same relations as in most regular
  filesystems, i.e. directories must exist for objects to be able to exist
  within them, directories may contain other directories or files, files cannot
  contain other objects, but can have contents stored as bytes
* paths that follow the [java.nio.file.Path][jdoc-path] specs and resolve to
  the correct [FileSystem][jdoc-fs] so that all operations can be performed
  through the [Files][jdoc-f] class
* reading and writing to files through [FileChannel][jdoc-fc]
* creating/deleting/moving files or directories using
  `Files.createDirectory(Path)`, `FileChannel.open(Path)`, `Files.delete(Path)`,
  `Files.move(Path, Path, CopyOption...)`, etc. methods
* walking through directory trees using [DirectoryStream][jdoc-ds]
* ZIP file support through nested [JDK.ZipFS][jdoc-zipfs] filesystem (WIP)
* [tests](https://github.com/k463/jnmofsexp1/actions/runs/17083855366?pr=1)! 🚨

[jdoc-ds]: https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/nio/file/DirectoryStream.html
[jdoc-f]: https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/nio/file/Files.html
[jdoc-fc]: https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/nio/channels/FileChannel.html
[jdoc-fs]: https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/nio/file/FileSystem.html
[jdoc-fsp]: https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/nio/file/spi/FileSystemProvider.html
[jdoc-nio-file]: https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/nio/file/package-summary.html
[jdoc-path]: https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/nio/file/Path.html
[jdoc-zipfs]: https://docs.oracle.com/en/java/javase/24/docs/api/jdk.zipfs/module-summary.html
