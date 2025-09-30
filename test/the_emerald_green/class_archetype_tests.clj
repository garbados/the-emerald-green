(ns the-emerald-green.class-archetype-tests
  "This file attempts to verify and ensure that
  each skill-major combination matches a class archetype
  and class archetypes are not used overmuch." 
  (:require
   [clojure.test :refer [deftest is testing]]
   [the-emerald-green.utils :refer [keyword->name]]))

(def skill->major->class
  {:athletics
   {:the-chariot :the-courier
    :the-hermit  :the-traveler}
   :melee
   {:strength    :the-fighter
    :the-hermit  :the-mystic
    :justice     :the-paladin}
   :ranged
   {:the-chariot :the-fighter
    :the-hermit  :the-ranger
    :the-empress :the-archer
    :death       :the-assassin}
   :resilience
   {:strength    :the-bulwark
    :temperance  :the-mystic}
   :intimidation
   {:the-devil   :the-villain}
   :arcana
   {:the-magician :the-wizard
    :the-world    :the-elementalist
    :the-sun      :the-pyromancer
    :death        :the-necromancer}
   :craft
   {:strength     :the-smith
    :the-empress  :the-builder
    :the-hermit   :the-survivor
    :the-devil    :the-poisoner}
   :diplomacy
   {:the-lovers   :the-comrade
    :the-devil    :the-spy
    :judgement     :the-inquisitor
    :temperance   :the-diplomat}
   :insight
   {:the-magician   :the-archivist
    :death          :the-seer
    :the-hanged-man :the-oracle
    :justice        :the-investigator
    :the-emperor    :the-tactician}
   :medicine
   {:the-lovers       :the-medic
    :the-moon         :the-poisoner
    :wheel-of-fortune :the-surgeon}
   :awareness
   {:the-hermit       :the-ranger
    :the-fool         :the-investigator}
   :deception
   {:the-tower        :the-rogue
    :the-devil        :the-illusionist
    :the-moon         :the-changeling}
   :sorcery
   {:the-magician     :the-blooded
    :the-moon         :the-witch}
   :stealth
   {:the-tower        :the-shadow
    :the-magician     :the-illusionist}
   :resolve
   {}
   :gambling
   {:the-fool         :the-bettor
    :the-emperor      :???}
   :appraisal
   {:wheel-of-fortune :the-merchant
    :the-tower        :the-fence
    :the-hierophant   :the-investigator}
   :skepticism
   {}
   :divination
   {}
   :theurgy
   {}})

(deftest overuse-tests
  (testing "Majors are not overused among skills."
    (let [wanted 3
          major->skills-actual
          (reduce
           (fn [acc [skill majors]]
             (reduce
              (fn [acc major]
                (if (nil? (get acc major))
                  (assoc acc major [skill])
                  (update acc major conj skill)))
              acc
              majors))
           {}
           skill->majors)]
      (doseq [[major skills] major->skills-actual
              :let [n (count skills)]]
        (is (= wanted n) (str (keyword->name major) " has " n " skills, not " wanted))))))
