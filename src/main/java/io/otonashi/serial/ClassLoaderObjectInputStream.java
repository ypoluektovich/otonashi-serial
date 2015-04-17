package io.otonashi.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

/**
 * ObjectInputStream that uses a fixed ClassLoader (specified at creation time).
 */
public class ClassLoaderObjectInputStream extends ObjectInputStream {

	protected final ClassLoader classLoader;

	/**
	 * Constructor for implementations that completely reimplement {@code ObjectInputStream}.
	 * See {@link java.io.ObjectInputStream#ObjectInputStream() ObjectInputStream()}.
	 */
	protected ClassLoaderObjectInputStream(ClassLoader classLoader) throws IOException, SecurityException {
		super();
		this.classLoader = classLoader;
	}

	/**
	 * Creates an object input stream that reads from the specified {@code InputStream} and
	 * resolves classes using the specified {@code ClassLoader}.
	 * A serialization stream header is read from the stream and verified.
	 * This constructor will block until the corresponding {@code ObjectOutputStream}
	 * has written and flushed the header.
	 *
	 * @param source the input stream to read from.
	 * @param classLoader the class loader to load classes with.
	 *
	 * @throws java.io.IOException from the ObjectInputStream
	 * {@linkplain java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream) constructor}.
	 * @throws java.lang.SecurityException from the ObjectInputStream
	 * {@linkplain java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream) constructor}.
	 * @throws java.lang.NullPointerException from the ObjectInputStream
	 * {@linkplain java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream) constructor}.
	 */
	public ClassLoaderObjectInputStream(InputStream source, ClassLoader classLoader) throws IOException {
		super(source);
		this.classLoader = classLoader;
	}

	/**
	 * Exposes the class loader used by this object input stream.
	 * @return the class loader specified at creation time.
	 */
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		return Class.forName(desc.getName(), false, classLoader);
	}

	@Override
	protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
		ClassLoader nonPublicLoader = null;
		boolean hasNonPublicInterface = false;

		Class[] classObjs = new Class[interfaces.length];
		for (int i = 0; i < interfaces.length; i++) {
			Class cl = Class.forName(interfaces[i], false, classLoader);
			if (!Modifier.isPublic(cl.getModifiers())) {
				if (hasNonPublicInterface) {
					if (nonPublicLoader != cl.getClassLoader()) {
						throw new IllegalAccessError("conflicting non-public interface class loaders");
					}
				} else {
					nonPublicLoader = cl.getClassLoader();
					hasNonPublicInterface = true;
				}
			}
			classObjs[i] = cl;
		}

		try {
			return Proxy.getProxyClass(
					hasNonPublicInterface ? nonPublicLoader : classLoader,
					classObjs
			);
		} catch (IllegalArgumentException e) {
			throw new ClassNotFoundException(null, e);
		}
	}

}
