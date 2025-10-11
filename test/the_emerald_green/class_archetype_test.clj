(ns the-emerald-green.class-archetype-test
  "This file attempts to verify and ensure that
  each skill-major combination matches a class archetype
  and class archetypes are not used overmuch." 
  (:require
   [clojure.string :as string]
   [clojure.test :refer [deftest is testing]]))

(def major->class-skills
  {:the-fool
   [:the-rogue
    [:resolve
     :arcana
     :deception
     :stealth
     :gambling]]
   :the-magician
   [:the-wizard
    [:arcana
     :sorcery
     :theurgy
     :divination
     :craft]]
   :the-high-priestess
   [:the-witch
    [:sorcery
     :skepticism
     :insight
     :medicine
     :diplomacy]]
   :the-empress
   [:the-summoner
    [:arcana
     :theurgy
     :medicine
     :resolve
     :resilience]]
   :the-emperor
   [:the-tactician
    [:ranged
     :athletics
     :appraisal
     :intimidation
     :resilience]]
   :the-hierophant
   [:the-cleric
    [:theurgy
     :divination
     :diplomacy
     :medicine
     :appraisal]]
   :the-lovers
   [:the-courtesan
    [:diplomacy
     :appraisal
     :deception
     :insight
     :skepticism]]
   :the-chariot
   [:the-ranger
    [:ranged
     :awareness
     :athletics
     :stealth
     :skepticism]]
   :strength
   [:the-warrior
    [:melee
     :ranged
     :athletics
     :resilience
     :craft]]
   :the-hermit
   [:the-outsider
    [:athletics
     :appraisal
     :skepticism
     :gambling
     :craft]]
   :the-wheel-of-fortune
   [:the-merchant
    [:appraisal
     :diplomacy
     :intimidation
     :gambling
     :deception]]
   :justice
   [:the-paladin
    [:melee
     :resilience
     :resolve
     :sorcery
     :medicine]]
   :the-hanged-man
   [:the-oracle
    [:sorcery
     :divination
     :insight
     :ranged
     :resolve]]
   :death
   [:the-alchemist
    [:arcana
     :theurgy
     :craft
     :medicine
     :gambling]]
   :temperance
   [:the-monk
    [:melee
     :resilience
     :insight
     :awareness
     :athletics]]
   :the-devil
   [:the-illusionist
    [:diplomacy
     :intimidation
     :deception
     :stealth
     :theurgy]]
   :the-tower
   [:the-occultist
    [:intimidation
     :divination
     :stealth
     :deception
     :ranged]]
   :the-star
   [:the-mesmerist
    [:sorcery
     :divination
     :skepticism
     :gambling
     :diplomacy]]
   :the-moon
   [:the-shadow
    [:stealth
     :melee
     :athletics
     :skepticism
     :awareness]]
   :the-sun
   [:the-exemplar
    [:resilience
     :resolve
     :insight
     :awareness
     :craft]]
   :judgment
   [:the-inquisitor
    [:arcana
     :divination
     :appraisal
     :resolve
     :intimidation]]
   :the-world
   [:the-wildshaper
    [:sorcery
     :awareness
     :insight
     :deception
     :melee]]})

(defn ^:no-stest group-classes-by-skill [class->skills]
  (reduce
   (fn [acc [class skills]]
     (reduce
      (fn [acc skill]
        (if (get acc skill)
          (update acc skill conj class)
          (assoc acc skill [class])))
      acc
      skills))
   {}
   class->skills))

(deftest class-skill-overuse-test
  (testing "How often is each skill used by a class?"
    (doseq [[skill classes]
            (group-classes-by-skill
             (vals
              major->class-skills))]
      (is (<= 5 (count classes) 6)
          (str (name skill) " used " (count classes) " times: " (string/join ", " (map name classes)))))))

#_(def skill->major->class
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
    :judgment     :the-inquisitor
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

#_(deftest overuse-tests
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
