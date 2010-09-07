package com.beust.jbus;

import com.beust.jbus.internal.Lists;
import com.beust.jbus.internal.Maps;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class JBus {
  private boolean m_verbose = false;

  private Map<Class<?>, List<Target>> m_subscribers = Maps.newHashMap();

  public void register(Object object) {
    for (Method m : object.getClass().getDeclaredMethods()) {
      Subscriber s = m.getAnnotation(Subscriber.class);
      if (s != null) {
        for (Class<?> type : m.getParameterTypes()) {
          p("Registering " + type + " with " + m);
          List<Target> targetList = m_subscribers.get(type);
          if (targetList == null) {
            targetList = Lists.newArrayList();
            m_subscribers.put(type, targetList);
          }
          Target target = new Target(object, m);
          targetList.add(target);
        }
      }
    }
  }

  public void unregister(Object object) {
    for (Map.Entry<Class<?>, List<Target>> set : m_subscribers.entrySet()) {
      List<Target> targets = set.getValue();
      List<Target> remove = Lists.newArrayList();
      for (Target t : targets) {
        // Note: use ==, not equals()
        if (t.getObject() == object) remove.add(t);
      }
      targets.removeAll(remove);
    }
  }

  private void p(String string) {
    if (m_verbose) {
      System.out.println("  [JBus] " + string);
    }
  }

  public void post(Object event) {
    post(event, new String[0]);
  }

  public void post(Object event, String[] categories) {
    p("Posted:" + event);
    List<Target> target = findTargets(event, categories);
    if (target != null) {
      for (Target t : target)
      {
        try {
          t.getMethod().invoke(t.getObject(), event);
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      }
    } else {
      p("No subscriber for event " + event);
    }
  }

  private List<Target> findTargets(Object event, String[] categories) {
    List<Target> result = Lists.newArrayList();

    // Find all the classes that either are equal or a subsclass of the event class
    for (Class<?> o : m_subscribers.keySet()) {
      Class<? extends Object> eventClass = event.getClass();
      if (o == event.getClass() || o.isAssignableFrom(eventClass)) {
        result.addAll(filterCategories(m_subscribers.get(o), categories));
        p("findTarget found base class target:" + o.getName() + " for event:" + event);
      }
    }

    return result;
  }

  private Collection<? extends Target> filterCategories(List<Target> list,
      String[] categories)
  {
    if (categories.length == 0) return list;

    List<Target> result = Lists.newArrayList();
    for (Target t : list) {
     String[] patterns = t.getCategoryPatterns();
     for (String pattern : patterns) {
       for (String category : categories) {
         if (Pattern.matches(pattern, category)) result.add(t);
       }
     }
    }

    return result;
  }

}