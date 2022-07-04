;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) UXBOX Labs SL

(ns app.common.test-helpers.files
  (:require
   [app.common.geom.point :as gpt]
   [app.common.types.components-list :as ctkl]
   [app.common.types.container :as ctn]
   [app.common.types.file :as ctf]
   [app.common.types.pages-list :as ctpl]
   [app.common.types.shape :as cts]
   [app.common.types.shape-tree :as ctst]
   [app.common.uuid :as uuid]))

(def ^:private idmap (atom {}))

(defn reset-idmap! []
  (reset! idmap {}))

(defn id
  [label]
  (get @idmap label))

(defn sample-file
  ([file-id page-id] (sample-file file-id page-id nil))
  ([file-id page-id props]
   (merge {:id file-id
           :name (get props :name "File1")
           :data (ctf/make-file-data file-id page-id)}
          props)))

(defn sample-shape
  [file label type page-id props]
  (ctf/update-file-data
    file
    (fn [file-data]
      (let [frame-id  (get props :frame-id uuid/zero)
            parent-id (get props :parent-id uuid/zero)
            shape     (if (= type :group)
                        (cts/make-minimal-group frame-id
                                                {:x 0 :y 0 :width 1 :height 1}
                                                (get props :name "Group1"))
                        (cts/make-shape type
                                        {:x 0 :y 0 :width 1 :height 1}
                                        props))]

        (swap! idmap assoc label (:id shape))
        (ctpl/update-page file-data
                          page-id
                          #(ctst/add-shape (:id shape)
                                           shape
                                           %
                                           frame-id
                                           parent-id
                                           0
                                           true))))))

(defn sample-component
  [file label page-id shape-id]
  (ctf/update-file-data
    file
    (fn [file-data]
      (let [page (ctpl/get-page file-data page-id)

            [component-shape component-shapes updated-shapes]
            (ctn/make-component-shape (ctn/get-shape page shape-id)
                                      (:objects page)
                                      (:id file))]

        (swap! idmap assoc label (:id component-shape))
        (-> file-data
            (ctpl/update-page page-id
                              #(reduce (fn [page shape] (ctst/set-shape page shape))
                                       %
                                       updated-shapes))
            (ctkl/add-component (:id component-shape)
                                (:name component-shape)
                                ""
                                shape-id
                                page-id
                                component-shapes))))))

(defn sample-instance
  [file label page-id library component-id]
  (ctf/update-file-data
    file
    (fn [file-data]
      (let [[instance-shape instance-shapes]
            (ctn/instantiate-component (ctpl/get-page file-data page-id)
                                       (ctkl/get-component (:data library) component-id)
                                       (:id library)
                                       (gpt/point 0 0))]

        (swap! idmap assoc label (:id instance-shape))
        (-> file-data
            (ctpl/update-page page-id
                              #(reduce (fn [page shape]
                                         (ctst/add-shape (:id shape)
                                                         shape
                                                         page
                                                         uuid/zero
                                                         (:parent-id shape)
                                                         0
                                                         true))
                                       %
                                       instance-shapes)))))))
