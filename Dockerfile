# Pull nginx base image
FROM nginx:latest

# Arguments
ARG STATIC_WEB_SERVER
ARG NGINX_PORT

# Copy server ngnix configuration template
COPY nginx.conf /etc/nginx/conf.d/site.template

# Eval arguments and copy custom configuration file from the current directory
RUN envsubst < /etc/nginx/conf.d/site.template > /etc/nginx/nginx.conf

#Start up nginx server
ENTRYPOINT ["nginx","-g","daemon off;"]
