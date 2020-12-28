---
author: Guillermo Robles
title: Build and run Kotlin in server side...
featuredImage: assets/media/posts/google-cloud-platform.png
permalink: ':year/:slug'
tags:
- guides
- kotlin
- ktor  
- server-side
- gcloud
draft: false
---
...with [Google Cloud Platform](https://cloud.google.com/).

"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."

{% highlight 'kotlin' %}

        public abstract class OrchidGenerator extends Prioritized implements OptionsHolder {

        protected final String key;
    
        protected final OrchidContext context;
    
        @Inject
        public OrchidGenerator(OrchidContext context, String key, int priority) {
            super(priority);
            this.key = key;
            this.context = context;
        }
    
        /**
         * A callback to build the index of content this OrchidGenerator intends to create.
         *
         * @return a list of pages that will be built by this generator
         */
        public abstract List<? extends OrchidPage> startIndexing();
    

        public abstract void startGeneration(Stream<? extends OrchidPage> pages);
    }
{% endhighlight %}

{% highlight 'kotlin' %}

        fun main() {
            val lo = 234
        }

{% endhighlight %}


