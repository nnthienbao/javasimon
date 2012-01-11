package org.javasimon;

import org.javasimon.callback.Callback;
import org.javasimon.callback.CompositeCallback;
import org.javasimon.utils.SimonUtils;

import java.util.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Implements fully functional {@link Manager} in the enabled state. Does not support
 * {@link #enable()}/{@link #disable()} - for this use {@link SwitchingManager}.
 *
 * @author <a href="mailto:virgo47@gmail.com">Richard "Virgo" Richter</a>
 */
public final class EnabledManager implements Manager {
	private final Map<String, AbstractSimon> allSimons = new HashMap<String, AbstractSimon>();

	private UnknownSimon rootSimon;

	private Callback callback = new CompositeCallback();

	private ManagerConfiguration configuration;

	/**
	 * Creates new enabled manager.
	 */
	public EnabledManager() {
		rootSimon = new UnknownSimon(ROOT_SIMON_NAME, this);
		allSimons.put(ROOT_SIMON_NAME, rootSimon);
		configuration = new ManagerConfiguration(this);
		callback.initialize();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Simon getSimon(String name) {
		return allSimons.get(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void destroySimon(String name) {
		if (name.equals(ROOT_SIMON_NAME)) {
			throw new SimonException("Root Simon cannot be destroyed!");
		}
		AbstractSimon simon = allSimons.remove(name);
		if (simon.getChildren().size() > 0) {
			replaceSimon(simon, UnknownSimon.class);
		} else {
			((AbstractSimon) simon.getParent()).replaceChild(simon, null);
		}
		callback.onSimonDestroyed(simon);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void clear() {
		allSimons.clear();
		rootSimon = new UnknownSimon(ROOT_SIMON_NAME, this);
		allSimons.put(ROOT_SIMON_NAME, rootSimon);
		callback.onManagerClear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Counter getCounter(String name) {
		return (Counter) getOrCreateSimon(name, CounterImpl.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Stopwatch getStopwatch(String name) {
		return (Stopwatch) getOrCreateSimon(name, StopwatchImpl.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Simon getRootSimon() {
		return rootSimon;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<String> getSimonNames() {
		return Collections.unmodifiableCollection(allSimons.keySet());
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({"unchecked"})
	@Override
	public Collection<Simon> getSimons(String pattern) {
		if (pattern == null) {
			return Collections.unmodifiableCollection((Collection) allSimons.values());
		}
		SimonPattern simonPattern = new SimonPattern(pattern);
		Collection<Simon> simons = new ArrayList<Simon>();
		for (Map.Entry<String, AbstractSimon> entry : allSimons.entrySet()) {
			if (simonPattern.matches(entry.getKey())) {
				simons.add(entry.getValue());
			}
		}
		return simons;
	}

	private synchronized Simon getOrCreateSimon(String name, Class<? extends AbstractSimon> simonClass) {
		// name can be null in case of "anonymous" Simons
		AbstractSimon simon = null;
		if (name != null) {
			if (name.equals(ROOT_SIMON_NAME)) {
				throw new SimonException("Root Simon cannot be replaced or recreated!");
			}
			simon = allSimons.get(name);
		}
		if (simon == null) {
			if (name != null && !SimonUtils.checkName(name)) {
				throw new SimonException("Simon name must match following pattern: '" + SimonUtils.NAME_PATTERN.pattern() + "', used name: " + name);
			}
			simon = newSimon(name, simonClass);
			callback.onSimonCreated(simon);
		} else if (simon instanceof UnknownSimon) {
			simon = replaceSimon(simon, simonClass);
			callback.onSimonCreated(simon);
		} else {
			if (!(simonClass.isInstance(simon))) {
				throw new SimonException("Simon named '" + name + "' already exists and its type is '" + simon.getClass().getName() + "' while requested type is '" + simonClass.getName() + "'.");
			}
		}
		return simon;
	}

	private AbstractSimon replaceSimon(AbstractSimon simon, Class<? extends AbstractSimon> simonClass) {
		AbstractSimon newSimon = instantiateSimon(simon.getName(), simonClass);
		newSimon.enabled = simon.enabled;

		// fixes parent link and parent's children list
		((AbstractSimon) simon.getParent()).replaceChild(simon, newSimon);

		// fixes children list and all children's parent link
		for (Simon child : simon.getChildren()) {
			newSimon.addChild((AbstractSimon) child);
			((AbstractSimon) child).setParent(newSimon);
		}

		allSimons.put(simon.getName(), newSimon);
		return newSimon;
	}

	private AbstractSimon newSimon(String name, Class<? extends AbstractSimon> simonClass) {
		AbstractSimon simon = instantiateSimon(name, simonClass);
		if (name != null) {
			addToHierarchy(simon, name);
			SimonConfiguration config = configuration.getConfig(name);
			if (config.getState() != null) {
				simon.setState(config.getState(), false);
			}
		}
		return simon;
	}

	private AbstractSimon instantiateSimon(String name, Class<? extends AbstractSimon> simonClass) {
		AbstractSimon simon;
		try {
			Constructor<? extends AbstractSimon> constructor = simonClass.getDeclaredConstructor(String.class, Manager.class);
			simon = constructor.newInstance(name, this);
		} catch (NoSuchMethodException e) {
			throw new SimonException(e);
		} catch (InvocationTargetException e) {
			throw new SimonException(e);
		} catch (IllegalAccessException e) {
			throw new SimonException(e);
		} catch (InstantiationException e) {
			throw new SimonException(e);
		}
		return simon;
	}

	private void addToHierarchy(AbstractSimon simon, String name) {
		allSimons.put(name, simon);
		int ix = name.lastIndexOf(HIERARCHY_DELIMITER);
		AbstractSimon parent = rootSimon;
		if (ix != -1) {
			String parentName = name.substring(0, ix);
			parent = allSimons.get(parentName);
			if (parent == null) {
				parent = new UnknownSimon(parentName, this);
				addToHierarchy(parent, parentName);
			}
		}
		parent.addChild(simon);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Callback callback() {
		return callback;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ManagerConfiguration configuration() {
		return configuration;
	}

	/**
	 * Throws {@link UnsupportedOperationException}.
	 */
	@Override
	public void enable() {
		throw new UnsupportedOperationException("Only SwitchingManager supports this operation.");
	}

	/**
	 * Throws {@link UnsupportedOperationException}.
	 */
	@Override
	public void disable() {
		throw new UnsupportedOperationException("Only SwitchingManager supports this operation.");
	}

	/**
	 * Returns true.
	 *
	 * @return true
	 */
	@Override
	public boolean isEnabled() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void message(String message) {
		callback.onManagerMessage(message);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void warning(String warning, Exception cause) {
		callback.onManagerWarning(warning, cause);
	}
}
